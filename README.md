# javacc-ext-cs.branch  
This is port of JavaCC version 6.0 with addition of support to generate .NET C# code.
The code generator is highly customizable based on code templates. Unfortunatelly such templates are not sufficent
to generate new language and source modifications are neccessary.

Sample grammars:

\test.tmp\cssStyle.jj  - grammar for parse CSS files.
\test.tmp\jsonCheck.jj  - grammar for decoding json files.

#IntelliJ branch
This is IntelliJ branch which moves project from NetBeans project system into IntelliJ project.

# Building project

1. Clone this branch into local repository
2. Run gradlew.bat (on Windows) gradlew (on Linux) with jar argument:
   > gradlew.bat jar


