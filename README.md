Manual for Running the Program
This manual provides a step-by-step guide to run and test the program. If you encounter issues,
ensure that the configuration and file generation scripts are correct.
1. Program Overview
The program implements a hierarchical P2P system with super-peers and leaf nodes, using
push-based and pull-based mechanisms to maintain file consistency. It supports testing different
TTR values for the pull-based approach and collecting statistics on query results.
---
3. Configuration Files
1. system
_
config.txt:
Define system-wide settings, including enabling/disabling push/pull mechanisms and the
default TTR value.
Example:
PUSH
ENABLED=true
_
PULL
ENABLED=true
_
TTR=30000
2. network
_
config.txt:
Define the network topology for super-peers and their connections with leaf nodes.
Example:
super-peer1: super-peer2,super-peer3
super-peer2: super-peer1,super-peer4
super-peer3-leaves: leaf1,leaf2
super-peer4-leaves: leaf3
4. T est Case
- Setup:
- Place files file1.txt, file2.txt in shared/leaf1/.
- Place files file3.txt, file4.txt in shared/leaf2/.
- T esting:
- T est the system with the following steps:
1. T est push-based invalidation by enabling PUSH
_
2. T est pull-based polling with varying TTR values:
ENABLED=true.
- TTR = 30 seconds
- TTR = 60 seconds
- TTR = 120 seconds
3. Observe logs for query results, invalidations, and modifications.
---
5. Running the Program
1. Compile all Java files:
javac src/*
.java
2. Run the Main program:
bash
java src.Main
3. Monitor the terminal for output logs.
---
6. Example Output
T erminal output will include:
1. Super-peer initialization:
plaintext
super-peer1 is running...
super-peer2 is running...
2. Leaf node activity:
leaf1: Loaded owned file file1.txt
leaf1: Registered file file1.txt with super-peer super-peer1
Push based results
super-peer10: Received POLL for file: file2.txt from leaf10
super-peer3: Received POLL for file: file7.txt from leaf3
super-peer9: Received POLL for file: file7.txt from leaf9
super-peer10: Received POLL for file: file7.txt from leaf10
super-peer3: Received POLL for file: file8.txt from leaf3
super-peer9: Received POLL for file: file8.txt from leaf9
super-peer10: Received POLL for file: file8.txt from leaf10
super-peer3: Received POLL for file: file10.txt from leaf3
super-peer9: Received POLL for file: file10.txt from leaf9
super-peer10: Received POLL for file: file10.txt from leaf10
super-peer3: Received POLL for file: file9.txt from leaf3
super-peer9: Received POLL for file: file9.txt from leaf9
super-peer10: Received POLL for file: file9.txt from leaf10
leaf3: T otal Queries: 31, Invalid Results: 0, Invalid Percentage: 0.0%
leaf3: T otal Queries: 64, Invalid Results: 0, Invalid Percentage: 0.0%
leaf2: T otal Queries: 31, Invalid Results: 0, Invalid Percentage: 0.0%
leaf8: T otal Queries: 30, Invalid Results: 0, Invalid Percentage: 0.0%
leaf9: T otal Queries: 63, Invalid Results: 0, Invalid Percentage: 0.0%
leaf10: T otal Queries: 60, Invalid Results: 0, Invalid Percentage: 0.0%
leaf1: T otal Queries: 87, Invalid Results: 0, Invalid Percentage: 0.0%
leaf5: T otal Queries: 87, Invalid Results: 0, Invalid Percentage: 0.0%
leaf3: T otal Queries: 87, Invalid Results: 0, Invalid Percentage: 0.0%
leaf6: Broadcast invalidation for file file10.txt
3. Polling and query results:
=== T esting with TTR: 30 seconds ===
leaf1: Polling for file file1.txt at super-peer1
leaf1: Response from server: VALID:file1.txt:30000
4. Statistics:
=== Statistics for TTR: 30 seconds ===
leaf1: T otal Queries: 40, Invalid Results: 2, Invalid Percentage: 5.0%
leaf2: T otal Queries: 42, Invalid Results: 3, Invalid Percentage: 7.1%
T o match the output:
- Ensure at least 2-3 querying leaf nodes and 1-2 modifying nodes.
- Generate files of varying sizes (e.g., file1.txt to file10.txt with sizes 1KB to 10KB)
8. Verifying Output
super-peer1 is running...
super-peer2 is running...
leaf1: Polling for file file1.txt at super-peer1
leaf1: Response from server: VALID:file1.txt:60000
leaf1: File file1.txt remains valid. New TTR: 60000ms
leaf2: Polling for file file3.txt at super-peer2
leaf2: Response from server: VALID:file3.txt:60000
leaf2: File file3.txt remains valid. New TTR: 60000ms
leaf1: T otal Queries: 50, Invalid Results: 1, Invalid Percentage: 2.0%
leaf2: T otal Queries: 52, Invalid Results: 2, Invalid Percentage: 3.8%
leaf3: T otal Queries: 49, Invalid Results: 5, Invalid Percentage: 10.2%
9. Troubleshooting
- Invalid Configuration:
- Ensure system
_
config.txt and network
_
config.txt are correctly formatted.
- Files Not Found:
- Verify that the files exist in the appropriate shared/ directories.
- Port Conflicts:
- Check that no other applications are using ports in the 8000+ range.
