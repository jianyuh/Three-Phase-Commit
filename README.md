===Three Phase Commit Protocol===
Contributors: Jianyu Huang, Xinyu Wang

==Description==

This project implements the 3PC Protocol for atomic commit in distributed systems. Specifically, it provides an interface for the users to maintain consistent local playlists with a set of songs in  distributed nodes. 

---Supported Operations---
It supports the follwing three operations on the playlists: 
1. add songName songURL coordinatorNum
   Ask the process with ProcNum = coordinatorNum to coordinate a transaction to make all the distributed nodes add a new song in their playlists, consistently.
2. delete songName songURL coordinatorNum
   Ask the process with ProcNum = coordinatorNum to coordinate a transaction to make all the distributed nodes delete an existing song, consistently.
3. edit songName newSongURL
   Ask the process with ProcNum = coordinatorNum to coordinate a transaction to make all the distributed nodes change the song URL of an existing song with songName to newSongURL, consistently.

---LOG---
Each node, no matter the coordinator or the participant, maintains a LOG which is stored in stable storage (disk). A node can, either independently or coorlaboratively, restore its playlist by the LOG. 
The log format is like: <TIME> <INITIAL/Start_3PC/YES/COMMIT/ABORT> <ADD/DELETE/EDIT> <songName> <songURL>

---Supported Failure Types and Test Cases---
This project supports the following failures: 
(In 1, 2, 5, there are totally 3 processes with ProcNum 0-2;
 In 3, 4, there are totally 4 processes with ProcNum 0-3)

1. Participant Failure and Recovery. 
   Process 0 (P0) performs as a coordinator and Process 1/2 (P1/2) perform as participants. 

TestCase1: 
   P1 crashes after sending "YES" to P0.
   P0 and P2 should COMMIT in the end of this transaction and update their uplists. 
   When P1 recovers, it should log "ABORT" and set the playlist to be NULL.
TestCase2: 
   P1 crashes after implement "COMMIT" and updates its playlist.
   P0 and P2 should commit and update their playlist consistently.
   When P1 recovers, it should do nothing to the LOG and recover its playlist by the LOG information.

2. Coordinator Failure and Recovery, including Partial COMMIT/ Fully COMMIT/ Partial Pre-commit/ Fully Pre-commit.
   Process 0 (P0) performs as a coordinator and Process 1/2 (P1/2) perform as participants.

TestCase1: 
   P0 crashes before broadcasting the VOTE_REQ message to anyone.
   P1 and P2 should consistently decide ABORT and do nothing to their playlists. 
   When P0 recovers, it should log "ABORT" and do nothing to the playlist.
TestCase2: 
   P0 crashes after broadcasting VOTE_REQ message to P1 and P2. 
   P1 and P2 should consistently decide ABORT and do nothing to their playlists. 
   When P0 recovers, it should log "ABORT" and do nothing to the playlist.
TestCase3:
   P0 crashes after sending PRE_COMMIT to P1 and P2. (Fully PRE_COMMIT)
   P1 and P2 should consistenly decide COMMIT and update their playlists.
   When P0 recovers, it should log "COMMIT" and update its playlist accordingly. 
TestCase4: 
   P0 crashes after sending PRE_COMMIT to P1. (Partial PRE_COMMIT)
   P1 and P2 should consistenly decide COMMIT and update their playlists. 
   When P0 recovers, it should log "COMMIT" and update its playlist accordingly.
TestCase5: 
   P0 crashes after sending COMMIT to P1 and P2. 
   P1 and P2 should commit and update the playlist.
   When P0 recovers, it should do nothing to the LOG and update the playlist according the LOG information.
TestCase6: 
   P0 crashes after sending COMMIT to P1. 
   P1 and P2 should commit and update the playlist. 
   When P0 recovers, it does nothing to the LOG and recovers its playlist.

3. Cascading Failure. 
   P0 performs as the coordinator at first, and if P0 fails, P1 will be elected, and then P2, finally P3.

TestCase1: 
   P0 crashes after sending VOTE_REQ to P1, P2, and P3. 
   Then, P1 is elected as a new coordinator, and will crash after sending STATE_REQ to P2 and P3. 
   Then, P2 is elected as a new coordinator, and will crash after sending STATE_REQ to P3. 
   Finally, P3 elects itself and ABORT in the end.

4. Future Coordinator Failure.
   P0 performs as the coordinator at first, and if P0 fails, P1 will be elected, and then P2, finally P3. 

TestCase1:   
   P0 crashes after sending PRE_COMMIT to P1, P2 and P3. 
   P1 crashes after sending YES to P0, which is before the time P0 crashes.
   P2 crashes aftet sending YES to P0, which is before the time P0 crashes.
   P3 should TIMEOUT on P0 when waiting for COMMIT, and then elects P1 to be the new coordinator, but TIMEOUT when waiting for the STATE_REQ message, and then elects P2 to be the new coordinator, and again TIMEOUT on P2 when waiting for the STATE_REQ message, and finally elects itself to decide COMMIT.

5. Total Failure. 
   Process 0 (P0) performs as a coordinator and Process 1/2 (P1/2) perform as participants.

TestCase1: 
   P0 crashes after sending PRE_COMMIT to P1 and P2. (UP_{P0}={P0,P1,P2})
   P1 crashes after voting YES, but before TIMEOUT on P0. (UP_{P1}={P0,P1,P2})
   P2 crashes after voting YES, but before TIMEOUT on P0. (UP_{P2}={P0,P1,P2})
   When P0, P1, P2 recovers, they cannot update their playlist and start a new transaction, until all of them are recovered.

TestCase2: 
   P0 crashes after sending VOTE_REQ. (UP_{P0}={P0,P1,P2})
   P1 crashes after TIMEOUT on P0 and sending STATE_REQ. (UP_{P1}={P1,P2})
   P2 crashes after receiving STATE_REQ and before TIMEOUT on P!. (UP_{P1}={P1,P2})
   When recovered, P1 and P2 can recover, but P0 and either P1 or P2 cannot. 

--Error in the Paper--

The coordinator should log PRE_COMMIT when sending out the PRE_COMMIT message.
