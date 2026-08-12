/**
 * Static Code Analysis Engine package.
 *
 * <p>Contains the core analysis pipeline: engine coordinator, language-specific
 * analyzers, rule engine, risk calculator, and the analyzer factory.</p>
 *
 * <p>Subpackages:</p>
 * <ul>
 *   <li>{@code analyzer.engine} — Scan orchestration (AnalyzerEngine, RuleEngine, RiskCalculator)</li>
 *   <li>{@code analyzer.parser} — Language analyzers (JavaAnalyzer, PythonAnalyzer, etc.)</li>
 *   <li>{@code analyzer.detector} — Individual vulnerability detector components</li>
 *   <li>{@code analyzer.rules} — Rule definitions (AbstractRule, SqlInjectionRule, etc.)</li>
 * </ul>
 */
package com.sanjay.aisecurity.analyzer;
