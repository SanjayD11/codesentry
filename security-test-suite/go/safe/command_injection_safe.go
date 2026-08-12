package main

import (
	"fmt"
	"net/http"
	"os/exec"
)

func commandHandler(w http.ResponseWriter, r *http.Request) {
	ip := r.URL.Query().Get("ip")
	
	// SAFE: Pass arguments properly
	safeCmd := exec.Command("ping", "-c", "4", ip)
	safeOut, _ := safeCmd.Output()
	fmt.Fprintf(w, string(safeOut))
}
