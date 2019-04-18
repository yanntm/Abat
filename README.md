# Abat
Action Based Attack Trees

### Introduction

This small project contains some proof of concept implementations of recent algorithms for analysis of action based attack trees.


![screenshot](https://raw.githubusercontent.com/yanntm/Abat/master/img/tree.png)


The examples and code are packaged as an eclipse plugin for ease of use and user friendliness.

## Installing

For install, within a recent eclipse (preferably 2018-12 or later), 
Help -> Install New Software -> Add this update site https://yanntm.github.io/Abat/

Check the category, approve the license, let eclipse restart.

## Getting started

Create a "File-> New-> Project > General :: Project"

Create a "File-> new -> File" and give it the `.abat` extension, e.g. `test.abat`.
Accept the dialog query for making this project "XText", this tool gives support for syntax highlighting, completion...

Alternatively, import some of the example files in the Examples folder of this repository.

## Tree syntax

Add a tree definition :

```
tree = ...  ;
```

The atoms are simply double quoted strings, corresponding to leaf actions.

The operators possible are : `AND, OR, SAND` and the weak variants `wAND, wSAND`.
They all take at least two and up to an arbitrary number of operands.

For instance :

```
tree = AND (
	OR ( SAND ("a1","a2","a3","a4","a5","a6") , SAND("a5","a6","a7","a8","a9")),
	OR ( SAND ("a1","a2","a3") , SAND("a4","a5","a6"), "a7" ),
	"a4"
	)
;
```

## Trace syntax

After the tree definition, define as many test traces as you wish, each of them can be named.

The syntax is a comma separated list of double quoted strings.

For instance :

```
trace interval_1_1 = "a1";
trace interval_1_2 = "a1","a2";
trace interval_1_3 = "a1","a2","a3";
trace interval_1_4 = "a1","a2","a3","a4";
trace interval_1_5 = "a1","a2","a3","a4","a5";
trace interval_1_6 = "a1","a2","a3","a4","a5","a6";
trace interval_1_7 = "a1","a2","a3","a4","a5","a6","a7";
trace interval_1_8 = "a1","a2","a3","a4","a5","a6","a7","a8";
trace interval_1_9 = "a1","a2","a3","a4","a5","a6","a7","a8","a9";
trace interval_1_10 = "a1","a2","a3","a4","a5","a6","a7","a8","a9","a10";
```

## Checking trace membership

To invoke the actual tool, right click the file and look for the menu entry "Test Semantics".
The tool answers with a diagnosis trace membership of each trace to the tree (accepted or rejected).

![screenshot](https://raw.githubusercontent.com/yanntm/Abat/master/img/screen1.png)

For instance 

```
Trace "interval_1_1" rejected 
Trace "interval_1_2" rejected 
Trace "interval_1_3" rejected 
Trace "interval_1_4" rejected 
Trace "interval_1_5" rejected 
Trace "interval_1_6" accepted 
Trace "interval_1_7" accepted 
Trace "interval_1_8" rejected 
Trace "interval_1_9" accepted 
Trace "interval_1_10" rejected 
```

(The double popup is a small workaround because the nicer second popup is not Copy/Paste enabled)

More diagnosis information can be found in the "console", use "Window->Show View->Other->Console" if it is not already displayed.

