const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const serverTimestamp = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_ethio_123";
const BOB_UID = "bob_ethio_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validProfile(uid) {
  return {
    userId: uid,
    displayName: "Abebe Bikila",
    email: "abebe@ethiogpt.et",
    preferredLanguage: "am",
    themeMode: "system",
    voiceAutoSpeak: false,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validConversation(uid) {
  return {
    userId: uid,
    title: "Amharic Study Help",
    toolMode: "tutor",
    language: "am",
    lastMessagePreview: "Selam! How can I help?",
    messageCount: 2,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validMessage(uid, role = "user") {
  return {
    userId: uid,
    role,
    text: "Explain the Axum obelisks in Amharic.",
    toolMode: "culture",
    hasImage: false,
    createdAt: serverTimestamp(),
  };
}

test("Unauthenticated user: cannot read or write users, conversations, or messages", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("conversations").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("conversations").doc("c1").collection("messages").get()
  );
});

test("Authenticated owner: can create, read, and update own user profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(profileRef.set(validProfile(ALICE_UID)));
  await assertSucceeds(profileRef.get());
  await assertSucceeds(
    profileRef.update({
      preferredLanguage: "om",
      themeMode: "dark",
      voiceAutoSpeak: true,
      updatedAt: serverTimestamp(),
    })
  );
});

test("Cross-user isolation: Bob cannot read or write Alice's profile, conversations, or messages", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("conversations").doc("conv1").set(validConversation(ALICE_UID))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("conversations").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("conversations").doc("conv2").set(validConversation(BOB_UID))
  );
});

test("Shadow update & validation guards: rejects ghost fields, invalid language, and immutable field tampering", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);

  // Ghost field rejected on create
  await assertFails(profileRef.set({ ...validProfile(ALICE_UID), isAdmin: true }));

  // Invalid language rejected
  await assertFails(profileRef.set({ ...validProfile(ALICE_UID), preferredLanguage: "xx" }));

  // Valid create succeeds
  await assertSucceeds(profileRef.set(validProfile(ALICE_UID)));

  // Ghost field rejected on update
  await assertFails(profileRef.update({ role: "admin", updatedAt: serverTimestamp() }));

  // Tampering userId rejected
  await assertFails(profileRef.update({ userId: BOB_UID, updatedAt: serverTimestamp() }));
});

test("Conversations and messages: valid lifecycle, parent gate, and append-only message immutability", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const convRef = aliceDb.collection("users").doc(ALICE_UID).collection("conversations").doc("conv_1");
  const msgRef = convRef.collection("messages").doc("msg_1");

  // Orphaned message without parent conversation fails
  await assertFails(msgRef.set(validMessage(ALICE_UID)));

  // Create parent conversation
  await assertSucceeds(convRef.set(validConversation(ALICE_UID)));

  // Create valid message under existing conversation
  await assertSucceeds(msgRef.set(validMessage(ALICE_UID)));

  // List own conversations and messages succeeds
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("conversations").get());
  await assertSucceeds(convRef.collection("messages").get());

  // Updating an existing message is forbidden (append-only)
  await assertFails(msgRef.update({ text: "Tampered text" }));

  // Deleting message and conversation by owner succeeds
  await assertSucceeds(msgRef.delete());
  await assertSucceeds(convRef.delete());
});
