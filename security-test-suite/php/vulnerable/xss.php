<?php

function greet() {
    $name = $_GET['name'];
    
    // VULNERABLE: XSS
    echo "Hello, " . $name;
}
?>
