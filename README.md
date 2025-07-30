![Lyricalia Banner](https://github.com/MarconiGRF/Lyricalia/blob/main/design/banner.png?raw=true)
# Lyricalia 🎶
A lyrics-based game between your friends!

https://github.com/user-attachments/assets/83e9c52e-f322-4e3e-b57e-5638a06f0801

## Game modes 🎮
- Complete the lyrics
    - Using commonly saved songs between the players, randomly picks songs and their lyrics, giving the players lyrics sections to be completed, the player that guesses the lyrics closer to the original ones wins more points!
- Guess the song (not implemented)
    - Using commonly saved songs between the players, randomly picks songs and their lyrics, giving the players complete sections of the lyrics, the player that guesses the song name & artist faster and more accurately wins more points!
- Random-time challenges (not implemented)
    - In a group of players, a challenge to either complete the lyrics or guess the song will appear in a random time of the day, the player that answers faster and morre accurately wins more points!

## Roadmap 🗺️
- [x] Implement client-agnostic API
- [x] Implement an Android Client
- [x] Implement Spotify library support
- [ ] Implement an iOS Client
- [ ] Implement Apple Music library support
- [ ] Implement a Discord Activity Client
- [ ] Implement a Web Client
- [ ] Implement YT Music library support

## Remarks ⚠️
- **This code is implemented as proof of concept, it by no means have the best code patterns or proper security-safe implementations due to numerous constraints.**
- You have to deploy the API in order to make the clients communicate properly
- The API must have a lyrics database modelled such as [lrclib](https://github.com/tranxuanthang/lrclib).
