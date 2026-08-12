const { exec } = require('child_process');

function executeCommand(ip) {
    // VULNERABLE: OS Command Injection
    exec('ping -c 4 ' + ip, (error, stdout, stderr) => {
        console.log(stdout);
    });
}
