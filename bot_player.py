import socket
import time
import random

# ----------------------------------------------------
#                SMART MAZE SOLVER BOT
# ----------------------------------------------------

SERVER_IP = "192.168.1.100"
UDP_PORT = 9876
BOT_ID = 777

print(f"--- SUPER SMART BOT STARTED ---")

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

# Movement vectors (maze navigation)
MOVE_VECTORS = {
    "UP": (0, -1),
    "DOWN": (0, 1),
    "LEFT": (-1, 0),
    "RIGHT": (1, 0)
}

# Opposites (prevent immediate backtracking)
OPPOSITE = {
    "UP": "DOWN",
    "DOWN": "UP",
    "LEFT": "RIGHT",
    "RIGHT": "LEFT",
    "NONE": "NONE"
}

# Internal memory of visited positions
visited = set()
stack = []           # DFS stack path
current_pos = (0, 0) # Bot assumes starting at 0,0
last_move = "NONE"

def send(direction):
    msg = f"MOVE;{BOT_ID};{direction}"
    sock.sendto(msg.encode("utf-8"), (SERVER_IP, UDP_PORT))


def get_next_position(pos, move):
    dx, dy = MOVE_VECTORS[move]
    return (pos[0] + dx, pos[1] + dy)


try:
    while True:

        visited.add(current_pos)

        # Build list of good moves (unvisited directions)
        good_moves = []
        for m in MOVE_VECTORS:
            newpos = get_next_position(current_pos, m)
            if newpos not in visited:
                good_moves.append(m)

        # Prioritize unvisited paths → TRUE INTELLIGENCE
        if good_moves:
            # remove instant backtracking if possible
            if last_move != "NONE" and OPPOSITE[last_move] in good_moves:
                if len(good_moves) > 1:
                    good_moves.remove(OPPOSITE[last_move])

            direction = random.choice(good_moves)
            stack.append(current_pos)  # Save path for backtracking

        else:
            # DEAD END → Backtrack like real maze solver
            if stack:
                prev = stack.pop()
                # find direction to go back
                for m in MOVE_VECTORS:
                    if get_next_position(current_pos, m) == prev:
                        direction = m
                        break
            else:
                # nowhere to go (very rare)
                direction = random.choice(list(MOVE_VECTORS.keys()))

        # Move
        send(direction)
        print(f"[BOT] {direction}")

        # Update memory state
        current_pos = get_next_position(current_pos, direction)
        last_move = direction

        # Extremely fast reaction
        time.sleep(0.03)

except KeyboardInterrupt:
    print("Bot stopped.")
    sock.close()

