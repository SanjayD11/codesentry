const axios = require('axios');
const fs = require('fs');
const FormData = require('form-data');
const path = require('path');

const baseURL = 'http://localhost:8081/api/v1';

const api = axios.create({ baseURL });
let token = '';

api.interceptors.request.use(config => {
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
});

async function runTests() {
    console.log('--- Phase 1: E2E API Workflow Validation ---');
    let projectId = '';
    let scanId = '';
    
    const email = `testuser_${Date.now()}@test.com`;
    const password = 'Password123!';
    
    // 1. Auth Registration
    try {
        await api.post(`/auth/register`, {
            firstName: 'Test',
            lastName: 'User',
            email,
            password
        });
        console.log('✅ Registration successful');
    } catch (e) {
        console.error('❌ Registration failed', e.response?.data);
    }
    
    // 2. Auth Login
    try {
        const res = await api.post(`/auth/login`, {
            email,
            password
        });
        token = res.data.data.token;
        console.log('✅ Login successful, got token:', token.substring(0, 15) + '...');
    } catch (e) {
        console.error('❌ Login failed', e.response?.data);
        return;
    }
    
    // 3. Project Creation
    try {
        const res = await api.post(`/projects`, {
            name: 'Test Project ' + Date.now(),
            description: 'Integration test project',
            projectType: 'WEB_APPLICATION'
        });
        projectId = res.data.data.id;
        console.log(`✅ Project created with ID: ${projectId}`);
    } catch (e) {
        console.error('❌ Project creation failed', e.response?.data);
    }
    
    // 4. Source Upload
    try {
        const formData = new FormData();
        const dummyFilePath = path.join(__dirname, 'test.py');
        fs.writeFileSync(dummyFilePath, 'import os\n\nos.system("ls") # Vulnerability\n');
        formData.append('files', fs.createReadStream(dummyFilePath));
        
        await api.post(`/projects/${projectId}/files`, formData, {
            headers: formData.getHeaders()
        });
        console.log('✅ File upload successful');
        fs.unlinkSync(dummyFilePath);
    } catch (e) {
        console.error('❌ File upload failed', e.response?.data);
    }
    
    // 5. Trigger Scan
    try {
        const res = await api.post(`/scans/${projectId}`);
        scanId = res.data.data.id || res.data.data.scanId;
        console.log(`✅ Scan triggered with ID: ${scanId}`);
    } catch (e) {
        console.error('❌ Scan trigger failed', e.response?.data);
    }
    
    // Wait for scan to complete
    if (scanId) {
        console.log('Waiting for scan to complete...');
        let status = 'IN_PROGRESS';
        let retries = 0;
        while ((status === 'IN_PROGRESS' || status === 'PENDING' || status === 'RUNNING') && retries < 20) {
            await new Promise(r => setTimeout(r, 2000));
            try {
                const res = await api.get(`/scans/report/${scanId}`);
                status = res.data.data.status;
                console.log(`Scan status: ${status}`);
            } catch {
                // Ignore errors during polling
            }
            retries++;
        }
        
        if (status === 'COMPLETED') {
            console.log('✅ Scan completed successfully');
            
            // 6. Check findings
            try {
                const res = await api.get(`/scans/report/${scanId}`);
                console.log(`✅ Scan returned ${res.data.data.totalVulnerabilities} vulnerabilities`);
            } catch (e) {
                console.error('❌ Failed to get scan findings', e.response?.data);
            }
        } else {
            console.error(`❌ Scan did not complete. Final status: ${status}`);
        }
    }
    
    console.log('--- Integration Testing Completed ---');
}

runTests();
