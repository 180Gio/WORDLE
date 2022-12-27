![](/images/logo.png)
## Introduction
WORDLE is a game in which the user has to guess a secret word every day.<br>
To guess, clues are given to users based on the letters of the word they enter:

- Letter **green**: letter present and in the correct position
- Letter **yellow**: letter present but not in the correct position
- Letter **red**: letter not present in the secret word

## Download
This repo is distributed under the GNU GPLv3 license.<br>

![](https://img.shields.io/badge/Server%20build-passing-success) 
![](https://img.shields.io/badge/Client%20build-passing-success)

## Building & executing
- Clone the repo: `git clone https://github.com/180Gio/WORDLE` 
- Install Java
- Go to the Server folder and compile: `javac -cp ".:./gson-2.10.jar" WordleServerMain.java WordleGame.java User.java TerminationHandler.java` <br>
(**NOTE**: if you are on Windows, replace `:` with `;`)
- Go to Client folder and compile: `javac WordleClientMain.java TerminationHandlerClient.java`
- Now you can run the Server (`java WordleServerMain` in Server folder) and the clients (`java WordleClientMain` in Client folder)
