# Abstract Debugging with GobPie demo (DEBT'24)

This folder contains the materials for the demo presentation of Abstract Debugging
with GobPie at the [Second Workshop on Future Debugging Techniques (DEBT'24)](https://conf.researchr.org/details/issta-ecoop-2024/debt-2024-papers/4/Abstract-Debugging-with-GobPie).

:movie_camera: There is a pre-recorded [video](https://youtu.be/KtLFdxMAdD8) of using the abstract debugger on the source code of SMTP Relay Checker, showcasing how to debug and fix a data race warning using the abstract debugger.

:page_facing_up: Cite with:
```
@inproceedings{10.1145/3678720.3685320,
  author    = {Holter, Karoliine and Hennoste, Juhan Oskar and Saan, Simmo and Lam, Patrick and Vojdani, Vesal},
  title     = {Abstract Debugging with GobPie},
  year      = {2024},
  isbn      = {9798400711107},
  publisher = {Association for Computing Machinery},
  address   = {New York, NY, USA},
  url       = {https://doi.org/10.1145/3678720.3685320},
  doi       = {10.1145/3678720.3685320},
  booktitle = {Proceedings of the 2nd ACM International Workshop on Future Debugging Techniques},
  pages     = {32–33},
  numpages  = {2},
  keywords  = {Abstract Interpretation, Automated Software Verification, Data Race Detection, Explainability, Visualization},
  location  = {Vienna, Austria},
  series    = {DEBT 2024}
}
```

### Examples

The directory contains the following example programs:

1. The [example program](./paper-example/example.c) illustrated in the paper.
2. An [extracted version](./smtprc-example) of the Smtp Open Relay Checker ([smtprc](https://sourceforge.net/projects/smtprc/files/smtprc/smtprc-2.0.3/)).
   Some parts of the code in this project were omitted to speed up the analysis for demonstration purposes.

Some illustrative example programs for demonstrating the abstract debugger's behavior <br>

- with [context-sensitive](context-sensitivity.c) analysis results;
- with [path-sensitive](path-sensitivity.c) analysis results;
- in case of function calls through [function pointers](fun-pointers.c).
