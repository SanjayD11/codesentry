const axios = require('axios');
const fs = require('fs');
const FormData = require('form-data');

async function run() {
  try {
    // 1. Login to get token
    console.log('Logging in...');
    const loginRes = await axios.post('https://codesentry-api-htph.onrender.com/api/v1/auth/login', {
      email: 'admin@codesentry.local', // Wait, I need a valid email/password.
      password: 'admin' // If admin@codesentry.local / admin is valid
    });
    
    // I can't guess the user's password, so this script might fail.
    // Instead of doing this, I'll ask the user to show me the Network tab response payload.
  } catch(e) {
    console.error(e.response ? e.response.data : e.message);
  }
}
run();
