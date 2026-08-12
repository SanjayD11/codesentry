import bcrypt from 'bcryptjs';

const hash = "$2a$10$YlJW6.fZxwMwgm7PubIJuumZHIyrpJ6g7WcGINnCjWzjkXI8C6HLG";
const passwords = ["admin", "password", "admin123", "testadmin", "123456", "12345678", "1234"];

passwords.forEach(p => {
    if (bcrypt.compareSync(p, hash)) {
        console.log("MATCH: " + p);
    }
});
