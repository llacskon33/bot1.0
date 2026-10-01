import contextlib
import io
import os
import sys
import traceback

_namespace = {"__name__": "__main__"}


def _run(source, working_directory, filename):
    output = io.StringIO()
    os.chdir(working_directory)
    if working_directory not in sys.path:
        sys.path.insert(0, working_directory)
    _namespace["__file__"] = filename
    try:
        with contextlib.redirect_stdout(output), contextlib.redirect_stderr(output):
            exec(compile(source, filename, "exec"), _namespace)
    except SystemExit as error:
        if error.code not in (None, 0):
            print(f"El programa terminó con código {error.code}", file=output)
    except Exception:
        traceback.print_exc(file=output)
    return output.getvalue()


def run_code(source, working_directory, filename="<console>"):
    return _run(source, working_directory, filename)


def run_file(path, working_directory):
    with open(path, encoding="utf-8") as program:
        source = program.read()
    return _run(source, working_directory, path)


def version():
    return sys.version.split()[0]
