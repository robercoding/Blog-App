const functions = require('firebase-functions');
const firebase_tools = require('firebase-tools');
const admin = require('firebase-admin');
admin.initializeApp();

exports.changeUsername = functions
    .https.onCall(async(data, context) => {

        const previousUsername = data.previousUsername
        const newUsername = data.newUsername
        const following = Number(data.following)
        const follower = Number(data.follower)
        const biography = data.biography
        const location = data.location
        const profileImageUrl = data.profileImageUrl
        const userId = data.userId
        const backgroundImageUrl = data.backgroundImageUrl
        const lastDateUsernameChange = Number(data.lastDateUsernameChange)

        //USERNAME
        const usernameSnapshotQuery = await admin.firestore().collection('usernames').where('username', '==', `${previousUsername}`).get();
        if (usernameSnapshotQuery.isEmpty) {
            return;
        }

        usernameSnapshotQuery.forEach(doc => {
            doc.ref.update({
                username: `${newUsername}`
            });
        });

        //USER
        const oldUserDoc = await admin.firestore().collection('users').doc(`${previousUsername}`).get();

        const newUserData = {
            username: `${newUsername}`,
            location: `${location}`,
            following: following,
            follower: follower,
            biography: `${biography}`,
            profileImageUrl: `${profileImageUrl}`,
            backgroundImageUrl: `${backgroundImageUrl}`,
            lastDateUsernameChange: lastDateUsernameChange,
            userId: `${userId}`
        }

        const newUserRef = admin.firestore().collection('users').doc(`${newUsername}`);
        newUserRef.update(newUserData, { merge: true });

        oldUserDoc.ref.delete()

        //POSTS
        /*         const userPostsSnapshotQuery = await admin.firestore().collection('posts').where('username', '==', `${previousUsername}`).get(); */
        const userPostsDoc = await admin.firestore().collection('posts').doc(userId).get();
        if (!userPostsDoc.exists) {
            return;
        }
        userPostsDoc.ref.update({ username: `${newUsername}` });
        /* userPostsSnapshotQuery.forEach(doc => {
            doc.ref.update();
        }); */

        //FOLLOWING 
        //Change username document of user following collection
        const userFollowingDoc = await admin.firestore().collection('following').doc(userId).get();
        if (!userFollowingDoc.exists) {
            return;
        }

        userFollowingDoc.ref.update({
            username: `${newUsername}`
        });

        //FOLLOWER
        //Change username document of user follower collection
        const userFollowerDoc = await admin.firestore().collection('follower').doc(userId).get();
        if (!userFollowerDoc.exists) {
            return;
        }

        userFollowerDoc.ref.update({
            username: `${newUsername}`
        });
        /* eslint-enable no-await-in-loop */
    });

/* exports.signUpWith = functions
    .https.onCall(async(data, context) => {
        const email = data.email;
        const password = data.password;
    });
 */
exports.changeUserDetails = functions
    .https.onCall(async(data, context) => {
        const username = data.username
        const newBiography = data.biography
        const newLocation = data.location
        const profileImageUrl = data.profileImageUrl
        const backgroundImageUrl = data.backgroundImageUrl

        const userCollectionRef = admin.firestore().collection('users')
        const userDocRef = userCollectionRef.doc(`${username}`)

        return await userDocRef.update({
            location: `${newLocation}`,
            biography: `${newBiography}`,
            profileImageUrl: `${profileImageUrl}`,
            backgroundImageUrl: `${backgroundImageUrl}`
        });
    });


//Disable in Auth and set disabled time in Firestore
exports.disableAccount = functions
    .https.onCall(async(data, context) => {

        const uid = context.auth.uid;

        const disabled = await admin.auth().updateUser(uid, {
            disabled: true
        }).then(function(userRecord) {
            if (userRecord.disabled) {
                return true;
            } else {
                return false;
            }
        }).catch(error => {
            functions.logger.log("Error!", error);
            throw new error
        });

        if (!disabled) {
            return false
        }

        const dateDisabledMilliseconds = new Date().getTime();
        const disabledObject = {
            dateDisabledMilliseconds: dateDisabledMilliseconds,
            disabled: true
        }
        await admin.firestore().collection('disabled').doc(uid).set(disabledObject);
        return true;
    });

exports.enableAccount = functions
    .https.onCall(async(data, context) => {

        const uid = data.uid;

        const disabled = await admin.auth().updateUser(uid, {
            disabled: false
        }).then(function(userRecord) {
            if (!userRecord.disabled) {
                return true;
            } else {
                return false;
            }
        }).catch(error => {
            functions.logger.log("Error!", error);
            throw new error
        });

        if (!disabled) {
            return false
        }

        const disabledObject = {
            dateDisabledMilliseconds: 0,
            disabled: false
        }
        await admin.firestore().collection('disabled').doc(uid).update(disabledObject);
        return true;
    });


exports.addNewPost = functions
    .https.onCall(async(data, context) => {

        const postTitle = data.postTitle;
        const postText = data.postText;
        const postLikes = Number(data.postLikes);
        const postCreatedAt = Number(data.postCreated_at);
        const postUserCreatorId = data.postUserCreatorId

        const postDocRef = admin.firestore().collection('posts').doc(postUserCreatorId).collection('user_posts').doc();

        await postDocRef
            .set({
                title: `${postTitle}`,
                text: `${postText}`,
                likes: postLikes,
                createdAt: postCreatedAt,
                postId: `${postDocRef.id}`,
                userCreatorId: `${postUserCreatorId}`
            });

        const countPostsDocRef = admin.firestore().collection('posts').doc(userPostsDocUid).collection('user_count_posts').doc('countPosts');
        return await countPostsDocRef.set({ countPosts: admin.firestore.FieldValue.increment(1) }, { merge: true })

    });


exports.updatePost = functions
    .https.onCall(async(data, context) => {
        const postID = data.postID;
        const title = data.title;
        const text = data.text;
        const userId = data.userId;

        return admin.firestore()
            .collection("posts").doc(userId)
            .collection("user_posts").doc(postID)
            .update({
                title: title,
                text: text
            }).then(() => {
                return true;
            }).catch(error => {
                return error;
            });
    });

exports.signUpWithEmail = functions
    .https.onCall(async(data, context) => {

        const username = data.username;
        const email = data.email;
        const password = data.password;

        functions.logger.log("Username", username);

        const isUsernameAvailableResult = await isUsernameAvailable(username);
        if (!isUsernameAvailableResult) {
            functions.logger.log("error", "Username isn't available");
            throw new functions.https.HttpsError('already-exists', 'Username is already picked up.');
        }

        const createUserAuthenticationResult = await createUserAuthentication(email, password);
        const status = createUserAuthenticationResult.status
        const uid = createUserAuthenticationResult.uid
        functions.logger.log("Status", status);
        functions.logger.log("uid", uid);

        if (!status) {
            functions.logger.log("error", "There was an error when creating the user");
            throw new functions.https.HttpsError('already-exists', 'Email is already picked up.');
        }

        if (uid === '' || uid === '0') {
            functions.logger.log("Error", "UID is not available");
            throw new functions.https.HttpsError('failed-precondition', 'UID is not available.');
        }

        const saveUsernameDocumentResult = await saveInUsernameCollection(uid, username, email)
        if (!saveUsernameDocumentResult) {
            functions.logger.log("Error", "Couldn't save in Username collection");
            throw new functions.https.HttpsError('failed-precondition', "Couldn't save in Username collection");
        }

        const saveUserDocumentResult = await saveInUserCollection(uid, username);
        if (!saveUserDocumentResult) {
            functions.logger.log("Error", "Couldn't save in Username collection");
            throw new functions.https.HttpsError('failed-precondition', "Couldn't save in Username collection");
        }

        await createDocumentsInCollection(uid, username, 'following');
        await createDocumentsInCollection(uid, username, 'follower');
        await createDocumentsInCollection(uid, username, 'posts');

        return true;
    });

async function isUsernameAvailable(username) {
    const usersCollection = admin.firestore().collection('users');

    const usersSnapshot = await usersCollection.where('username', '==', username).get();

    if (usersSnapshot.size > 0) {
        return false;
    } else {
        return true;
    }
}

async function createUserAuthentication(emailUser, passwordUser) {
    return admin.auth().createUser({
        email: emailUser,
        password: passwordUser,
        emailVerified: false,
        disabled: false
    }).then(userRecord => {
        if (userRecord.email !== emailUser) {
            return {
                "status": false,
                "uid": "0"
            }
        }
        return {
            "status": true,
            "uid": userRecord.uid
        }
    }).catch(error => {
        return error;
    });
}

async function saveInUsernameCollection(uid, username, email) {

    const usernameObject = {
        uid: uid,
        username: username,
        email: email
    };

    const usernameCollection = admin.firestore().collection("usernames").doc(uid);

    return await usernameCollection.set(usernameObject)
        .then(() => {
            return true;
        }).catch(error => {
            return false;
        });
}

async function saveInUserCollection(uid, username) {
    const userObject = {
        userId: uid,
        username: username,
        biography: "",
        location: "",
        following: 0,
        follower: 0,
        backgroundImageUrl: "",
        profileImageUrl: "",
        lastDateUsernameChange: 0
    };

    const userCollection = admin.firestore().collection("users").doc(username);

    return await userCollection.set(userObject)
        .then(() => {
            return true;
        }).catch(error => {
            return false;
        });
}

async function createDocumentsInCollection(uid, username, nameCollection) {

    return await admin.firestore()
        .collection(nameCollection)
        .doc(uid)
        .set({
            username: username
        });
}

exports.checkIfAccountIsDisabled = functions
    .https.onCall(async(data, context) => {
        const uid = data.uid;

        /*         admin.auth().getUser({
                }) */

        const docDisabled = await admin.firestore().collection('disabled').doc(uid).get();

        if (!docDisabled.exists) {
            return {
                disabled: false
            };
        }

        const dateNow = new Date().getTime();
        const dateDisabled = docDisabled.data().dateDisabledMilliseconds;


        if (dateDisabled === '' || dateDisabled === null) {
            const VALUE_NOT_FOUND = 101
            return {
                disabled: true,
                errorCode: VALUE_NOT_FOUND
            };
            /*             throw new functions.https.HttpsError('notFound', "There's not such value in our database"); */
        }

        const dateSecondsDifference = (dateNow - dateDisabled) / 1000;
        const dayDifference = (dateSecondsDifference / 86400);

        //ERRORS
        const ACCOUNT_DISABLED_LESS_30_DAYS = 3
        const ACCOUNT_DISABLED_MORE_30_DAYS = 4
        if (dayDifference <= 30) {
            return {
                disabled: true,
                errorCode: ACCOUNT_DISABLED_LESS_30_DAYS
            };
        } else {
            return {
                disabled: true,
                errorCode: ACCOUNT_DISABLED_MORE_30_DAYS
            };
        }
    });

exports.getUidByEmail = functions
    .https.onCall(async(data, context) => {

        const email = data.email;
        const usernameSnapshotQuery = await admin.firestore().collection("usernames").where('email', '==', email).get();
        if (usernameSnapshotQuery.empty)
            throw functions.https.HttpsError('value-not-found', "We couldn't find an user with that email");

        var uid = ''
        usernameSnapshotQuery.forEach(doc => {
            uid = doc.data().uid
        });

        if (uid === '' || uid === null) {
            functions.logger.log('Throw')
            throw functions.https.HttpsError('value-not-found', "We couldn't find an user with that email");
        }
        return {
            status: true,
            uid: uid
        }
        /* 
                return await admin.auth()
                    .getUser(uid)
                    .then(userRecord => {
                        return {
                            status: true,
                            email: userRecord.email
                        }
                    }).catch(error => {
                        return {
                            status: false,
                            error: error
                        }
                    }); */
    });

exports.deleteUser = functions
    .https.onCall(async(data, context) => {
        const uid = data.uid
        return await admin.auth().deleteUser(uid)
            .then(() => {
                return true;
            })
            .catch(error => {
                return error;
            });
    });

exports.onDeleteUserTrigger = functions
    .auth.user().onDelete((user) => {
        const uid = user.uid;

        return onDeleteUser(uid);
    });

async function onDeleteUser(uid) {

    //User
    const usersSnapshotQuery = await admin.firestore().collection('users').where('userId', '==', uid).get();

    usersSnapshotQuery.forEach(doc => {
        doc.ref.delete();
        functions.logger.log('Deleted User');
    });


    //Username
    await admin.firestore().collection('usernames').doc(uid).delete();

    //Reports
    await admin.firestore().collection('reports').doc(uid).delete();

    functions.logger.log('Delete followings');
    //Following
    //Get all followingID to delete the current uid in Follower of other users
    const followingSnapshotQuery = await admin.firestore().collection('following').doc(uid).collection('user_following').get();
    const listFollowingID = [];
    if (!followingSnapshotQuery.empty) {
        followingSnapshotQuery.forEach(doc => {
            listFollowingID.push(doc.data().followingId);
        });
    }

    const followingPromises = [];
    for (const followingID of listFollowingID) {
        followingPromises.push(deleteIdInFollowerCollection(followingID, uid));
    }
    functions.logger.log('FollowingPromises LENGTH:', followingPromises.length);

    await Promise.all(followingPromises);

    //Delete followingID
    deleteNestedDocument(`following/${uid}`)
    await admin.firestore().collection('following').doc(uid).delete();

    //Follower
    //Get all followerID to delete currentID in Following of other users
    const followerSnapshotQuery = await admin.firestore().collection('follower').doc(uid).collection('user_followers').get();
    const listFollowerID = [];
    if (!followerSnapshotQuery.empty) {
        followerSnapshotQuery.forEach(doc => {
            listFollowerID.push(doc.data().followerId);
        });
    }

    const followerPromises = [];
    for (const followerID of listFollowerID) {
        followerPromises.push(deleteIdInFollowingCollection(followerID, uid));
    }

    await Promise.all(followerPromises)

    //Delete Nested FollowerID 
    deleteNestedDocument(`follower/${uid}`)
    await admin.firestore().collection('follower').doc(uid).delete();

    //Disabled
    await admin.firestore().collection('disabled').doc(uid).delete();

    return true;
}

async function deleteIdInFollowerCollection(followingID, uid) {
    const docSnapshotFollower = await admin.firestore().collection('follower').doc(followingID).collection('user_followers').where('followerId', '==', uid).get();
    if (docSnapshotFollower.empty) {
        return;
    }
    docSnapshotFollower.forEach(doc => {
        doc.ref.delete();
    });
}

async function deleteIdInFollowingCollection(followerID, uid) {
    const docSnapshotFollowing = await admin.firestore().collection('following').doc(followerID).collection('user_following').where('followingId', '==', uid).get();
    if (docSnapshotFollowing.empty) {
        return;
    }
    docSnapshotFollowing.forEach(doc => {
        doc.ref.delete();
    });
}

async function deleteNestedDocument(path) {

    await firebase_tools.firestore
        .delete(path, {
            project: process.env.GCLOUD_PROJECT,
            recursive: true,
            yes: true,
            token: functions.config().fb.token
        });

    return true;
}