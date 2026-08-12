-- Run this script in your database client (e.g. DBeaver, MySQL Workbench, or Aiven console) to reset the password for testadmin@example.com to 'admin123'
UPDATE defaultdb.users 
SET password='$2a$10$.KfcwBFULu4RXBhiWxF6Ge23Q6bwhtgYG3JNT3KEt.qeVUEiO4D8S' 
WHERE email='testadmin@example.com';
