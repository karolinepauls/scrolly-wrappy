# scrolly-wrappy

Reagent component to provide a friendly drag-to-scroll experience for desktop.

## Overview

The component wraps arbitrarily long and wide content, providing horizontal drag-to-scroll within
the boundaries of the wrapper and vertical drag-to-scroll synchronised with the entire page.

## Dev/demo Setup

To get an interactive development environment run:

    lein figwheel

and open your browser at [localhost:3449](http://localhost:3449/).
This will auto compile and send all changes to the browser without the
need to reload. After the compilation process is complete, you will
get a Browser Connected REPL. An easy way to try it is:

    (js/alert "Am I connected?")

and you should see an alert in the browser window.

To clean all compiled files:

    lein clean

To create a production build run:

    lein do clean, cljsbuild once min

And open your browser in `resources/public/index.html`. You will not
get live reloading, nor a REPL. 

## Acknowledgements

The scrolly-wrappy component has been extracted from https://github.com/infectious/ephyra.

## License

See LICENCE.

Copyright © 2019 Karoline Pauls, Infectious Media
