# A zero-dependency EBNF parser generator

## How to use
First make sure the parser generator is built:
```bash
./gradlew fatjar
```

Then, make it generate the parser from your EBNF grammar file:
```bash
java -jar cli/build/libs/parser-gen-cli-0.1.0.jar -g <your_grammar_file> -o your/package/MyParser
```
This will create a java file called `MyParser.java` inside the existing `your/package` directory.

Finally, use it in your code like so:
```bash
package your.package;
public class MyClass {
	public static void main(String[] args) {
		final String text = Files.readString(args[0]);
		System.out.println(MyParser.parse(text));
	}
}
```

## How to contribute
```bash
./gradlew build
```
