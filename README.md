# Java Chemical Nomenclature System 

This is a Java-based application for parsing and naming simple organic molecules using Czech nomenclature conventions. It includes a graphical interface for molecule input and generates systematic names in Czech.

Developed as a student project at Charles University (MFF UK).

## Features

- SMILES parser for basic organic compounds
- Czech-language systematic name generation
- Swing GUI for molecule input and live name preview
- Modular object-oriented architecture 
- Unit tests 
- Maven-based build system
- Javadoc documentation (generate locally)

## Requirements

- Java 17 
- Apache Maven

## Build the Project

```bash
mvn clean install
``` 

## Run the GUI Application

```bash
mvn exec:java
``` 

## Run Unit Tests

```bash
mvn test
``` 

## Generate Javadoc

```bash
mvn clean javadoc:javadoc
``` 

## Included Documents
- dokumentace.pdf – Technical documentation (in Czech)
- uzivatelska_prirucka.pdf – User guide (in Czech)
