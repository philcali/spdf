# SPDF

This simple utility wraps [iText][1] and [flying saucer][2] to quickly create
PDF's from the commandline.

## Installation

```
cs philcali/spdf
```

## Usage

```
sdpf -h [file|folder|url...] [-o out.pdf]
```

The `url` must be in the form of `*://*`. The specified output file must be in
the form of `*.pdf*`.

The utility was designed to place nice with other CL tools, so input and output
can be piped and redirected (in fact, it is encouraged to do so).

```
cat flyer.html | spdf > out.pdf

curl www.google.com | sdf > google.pdf

lmxml flyer.lmxml | sdf > out.pdf
```

## Creating Multiple Pages

It's possible to create a multiple page document from a sequence of
interspersed files, folders, or URL's by adding them to the var args.
For example:

```
spdf http://google.com /home/philip/flyer.pdf > branding.pdf

spd /my/book > book.pdf
```

## License

Because spdf uses iText for PDF conversion, this application must also be
licensed under [AGPL][3].

Read the LICENSE for spdf's copyright.

[1]: http://itextpdf.com/itext.php
[2]: https://github.com/flyingsaucerproject/flyingsaucer#readme
[3]: http://itextpdf.com/terms-of-use/agpl.php
