import sys
import json
import os
import pyttsx3
import time

# Print Python executable and path for debugging
print("Python executable:", sys.executable)
print("Python path:", sys.path)

# Read JSON input from stdin
input_data = json.load(sys.stdin)

# Extract fields
name = input_data["name"]
amount = input_data["amount"]
date = input_data["date"]
template = input_data["template"]
text = input_data["text"]

# Optional: Replace placeholders if needed
# text = text.replace("${name}", name).replace("${amount}", amount).replace("${date}", date)

# Initialize pyttsx3 engine
engine = pyttsx3.init()
engine.setProperty('rate', 150)   # Moderate speech rate
engine.setProperty('volume', 1.0) # Max volume

# Define output directory and file path
output_dir = os.path.join("public", "voiceovers")
os.makedirs(output_dir, exist_ok=True)

output_filename = f"voiceover_{template}_{name}.mp3"
output_path = os.path.join(output_dir, output_filename)

# Save speech to file
engine.save_to_file(text, output_path)
engine.runAndWait()

# Wait for file to be written (basic sync)
timeout = 5
start_time = time.time()
while (not os.path.exists(output_path) or os.path.getsize(output_path) == 0) and (time.time() - start_time < timeout):
    time.sleep(0.1)

# Confirm the voiceover was saved
if os.path.exists(output_path) and os.path.getsize(output_path) > 0:
    print(f"Voiceover saved to {output_path}")
else:
    print(f"Failed to generate voiceover at {output_path}")
    sys.exit(1)
