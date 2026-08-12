import os
import subprocess

def execute_command(user_input):
    # VULNERABLE: Direct execution of user input with shell=True
    os.system("ping " + user_input)
    subprocess.run("ping " + user_input, shell=True)
