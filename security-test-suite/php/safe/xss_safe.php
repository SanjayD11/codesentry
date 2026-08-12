<?php

function greet() {
    $name = $_GET['name'];
    
    // SAFE: Escaped Output
    echo "Hello, " . htmlspecialchars($name, ENT_QUOTES, 'UTF-8');
}
?>
