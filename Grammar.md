# Grammar

```
regex: concat union;
union: '|' concat union;
union: ε;
concat: kleene suffix;
suffix: kleene suffix;
suffix: ε;
kleene: base star;
star: '*';
star: ε;
base: LIT;
base: '(' regex ')';
base: '[' negation inhalt range ']';
negation: '^';
negation: ε;
range: inhalt range;
range: ε;
inhalt: LIT rest;
rest: '-' LIT;
rest: ε;
```

# First-, Follow- and Select-Set

| RULE                                 | FIRST   | FOLLOW       | SELECT         |
|--------------------------------------|---------|--------------|----------------|
| regex: concat union;                 | LIT ( [ | $            | LIT ( [        |
| union: '\|' concat union;            | \|      |              | \|             |
| union: ε;                            | ε       | $)           | $)             |
| concat: kleene suffix;               | LIT ( [ |              | LIT ( [        |
| suffix: kleene suffix;               | LIT ( [ |              | LIT ( [        |
| suffix: ε;                           | ε       | ) \| $       | ) \| $         |
| kleene: base star;                   | LIT ( [ |              | LIT ( [        |
| star: '*';                           | *       |              | *              |
| star: ε;                             | ε       | $ LIT ( [ \| | $ LIT ( ) [ \| |
| base: LIT;                           | LIT     |              | LIT            |
| base: '(' regex ')';                 | (       |              | (              |
| base: '[' negation inhalt range ']'; | [       |              | [              |
| negation: '^';                       | ^       |              | ^              |
| negation: ε;                         | ε       | LIT          | LIT            |
| range: inhalt range;                 | LIT     |              | LIT            |
| range: ε;                            | ε       | ]            |         ]         |
| inhalt: LIT rest;                    | LIT     |              | LIT            |
| rest: '-' LIT;                       | -       |              | -              |
| rest: ε;                             | ε       | LIT ]        | LIT ]          |:w

