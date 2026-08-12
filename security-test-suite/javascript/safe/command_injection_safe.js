const { execFile } = require('child_process');

function executeCommand(ip) {
    // SAFE: Use execFile with arguments
    execFile('ping', ['-c', '4', ip], (error, stdout, stderr) => {
        console.log(stdout);
    });
}
