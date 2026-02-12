# 🎰 Bingo Server Gold - Real Money System

A comprehensive Java-based multiplayer Bingo application designed for real-time gameplay over Local Area Networks (LAN) or the Internet. The system features an advanced Host Dashboard and an interactive Player Dashboard with live balance and bet management.

## 🚀 Key Features
* **Client-Server Architecture:** Seamless communication via TCP Sockets.
* **Multi-User Management:** Support for multiple concurrent players with live tracking of balances and bets in the Hall.
* **Betting System:** Automated prize pool management, with instant credits for "Cinquina" (Line) and Bingo wins.
* **Graphical User Interface (Swing):** A master scoreboard for the Host and interactive tickets with automatic "X" marking for Players.
* **Payout Integrity:** Synchronized server logic ensures unique prize assignment, preventing duplicate or erroneous payouts.

## 🛠️ Requirements
* **Java Runtime Environment (JRE) 17** or higher.
* **Radmin VPN** or **Hamachi** (recommended for Internet play).

## 🎮 How to Play
1.  Download the `BingoGame.jar` file.
2.  Launch the file (double-click or run `java -jar BingoGame.jar`).
3.  **For the Host:** Select "CREATE GAME," share the displayed IP address with your friends, and click "REQUEST BETS" once everyone has joined.
4.  **For Players:** Select "JOIN GAME," enter the Host's IP address, and provide your name.

## 📦 Compilation (For Developers)
To compile the project manually:
```bash
javac -encoding UTF-8 *.java
echo Main-Class: BingoLauncher > manifest.txt
jar cmf manifest.txt BingoGame.jar *.class icon.png
