package main

import (
	"fmt"
	"net/http"
	"os/exec"
)

func commandHandler(w http.ResponseWriter, r *http.Request) {
	ip := r.URL.Query().Get("ip")
	
	// VULNERABLE: OS Command Injection
	cmd := exec.Command("sh", "-c", "ping -c 4 "+ip)
	out, _ := cmd.Output()
	fmt.Fprintf(w, string(out))
}
