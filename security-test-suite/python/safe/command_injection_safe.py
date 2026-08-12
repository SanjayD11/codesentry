import subprocess

def execute_command(user_input):
    # SAFE: shell=False with argument list
    subprocess.run(["ping", user_input], shell=False)
