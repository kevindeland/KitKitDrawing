## KitKit Drawing

copied from [here](https://github.com/XPRIZE/GLEXP-Team-KitkitSchool).

Modified by Kevin DeLand

---

### Drawing Modes and how to use them

#### Radial
1. tap the Radial button to switch between 1, 2, or 8 radials <IMAGE>
2. when touch down, it records the origin. Wherever you draw next, it will copy that, but angle-shifted by 360/N degrees. (So for 8, it will copy what you draw every 45 degrees).
3. When you release, the origin is lost.

This is good for drawing many single use things with radial symmetry, e.g. many flowers or fireworks (see below).


#### Parallel drawing
This has a cycle of two modes...

1. Place anchors where you want to draw
2. Draw from the first anchor (highlighted), and it will draw translated (x,y) from the rest.


#### Angled Parallel drawing
Like parallel drawing, but you can draw at different angles.

1. Place a vector anchor by placing down, and dragging your finger in the direction you want it to face.
2. Continue placing as many vectors as you want.
3. When ready to draw, switch to draw mode and then draw from the first vector anchor (highlighted). It will draw translated (x,y) and rotated (theta) in each vector location.


### Copyright 2018 Enuma, Inc
Licensed under the Apache License 2.0 and the Creative Commons Attribution International 4.0 License (the “Licenses”); you may not use these files except in compliance with the Licenses. 