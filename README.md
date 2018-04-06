# COMP90015
project
eclipse commmands for server: 
1. initial a server A : -lh localhost -lp 3780 -s secret
2. start Server B and connect to server A: -lh localhost -lp 3781 -rh localhost -rp 3780 -s secret
3. start Server C and connect to server A: -lh localhost -lp 3782 -rh localhost -rp 3780 -s secret
...


Server A: localhost 3780 ; Server B: localhost 3781 ; Server C: localhost 3782 
