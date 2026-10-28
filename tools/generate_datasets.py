#!/data/data/com.termuxpro.app/files/usr/bin/python3
import json
import os
import glob
import unicodedata

# 1. Build >= 600 telex_engine_cases.json
mandatory_cases = [
    # Required in specification
    {"input": "thuongw", "expected": "thương"},
    {"input": "thuowng", "expected": "thương"},
    {"input": "nguoiwf", "expected": "người"},
    {"input": "nguwowif", "expected": "người"},
    {"input": "duocj", "expected": "được"},
    {"input": "truongwf", "expected": "trường"},
    {"input": "nhuwngx", "expected": "những"},
    {"input": "Vieetj", "expected": "Việt"},
    {"input": "Vietj", "expected": "Việt"},
    {"input": "quoocs", "expected": "quốc"},
    {"input": "nguyeenx", "expected": "nguyễn"},
    {"input": "huowngs", "expected": "hướng"},
    {"input": "muowjn", "expected": "mượn"},
    {"input": "muowfn", "expected": "mườn"},
    {"input": "khuyeens", "expected": "khuyến"},
    {"input": "thuyr", "expected": "th\u1ee7y"},
    {"input": "hoaf", "expected": "ho\u00e0"},
    {"input": "Dduwowcj", "expected": "Được"},
    {"input": "ddi", "expected": "đi"},
    {"input": "Ddaf", "expected": "Đà"},
    {"input": "Nangwx", "expected": "Nẵng"},
    {"input": "aaa", "expected": "aa"},
    {"input": "ww", "expected": "w"},
    {"input": "ss", "expected": "s"},
    {"input": "thuong7", "expected": "thương"},
    {"input": "nguoi72", "expected": "người"},
    {"input": "truong72", "expected": "trường"},
    {"input": "duoc75", "expected": "được"},
    {"input": "nhung74", "expected": "những"},
    {"input": "Viet65", "expected": "Việt"},
    {"input": "quoc61", "expected": "quốc"},
    {"input": "asf", "expected": "à"},
]

# Generate variations across vowels, initials, tones, and finals
initials = ["", "b", "c", "ch", "d", "dd", "g", "gh", "gi", "h", "k", "kh", "l", "m", "n", "ng", "ngh", "nh", "p", "ph", "qu", "r", "s", "t", "th", "tr", "v", "x"]
tones_map = [("", 0), ("s", 1), ("f", 2), ("r", 3), ("x", 4), ("j", 5)]

# Base vowel patterns: (telex_input, expected_char)
vowels_telex = [
    ("a", "a"), ("aa", "â"), ("aw", "ă"),
    ("e", "e"), ("ee", "ê"),
    ("i", "i"),
    ("o", "o"), ("oo", "ô"), ("ow", "ơ"),
    ("u", "u"), ("uw", "ư"),
    ("y", "y"),
    ("ia", "ia"), ("ie", "ie"), ("ua", "ua"), ("uow", "ươ"), ("uwo", "ươ"),
    ("ai", "ai"), ("ao", "ao"), ("au", "au"), ("ay", "ay"),
    ("eo", "eo"), ("eu", "êu"), ("iu", "iu"),
    ("oi", "oi"), ("ooi", "ôi"), ("owi", "ơi"),
    ("ui", "ui"), ("uwi", "ưi"), ("uy", "uy"),
    ("oa", "oa"), ("oe", "oe"),
]

finals = ["", "c", "m", "n", "ng", "nh", "p", "t"]

cases = list(mandatory_cases)
seen_inputs = set(c["input"] for c in cases)

# Generate comprehensive combinations
sample_words = [
    # Basic tones on monophthongs
    ("toan", "toans", "toán"), ("toan", "toanf", "toàn"), ("toan", "toanr", "toản"), ("toan", "toanx", "toãn"), ("toan", "toanj", "toạn"),
    ("lang", "langs", "láng"), ("lang", "langf", "làng"), ("lang", "langr", "lảng"), ("lang", "langx", "lãng"), ("lang", "langj", "lạng"),
    ("nam", "nams", "nám"), ("nam", "namf", "nàm"), ("nam", "namr", "nảm"), ("nam", "namx", "nãm"), ("nam", "namj", "nạm"),
    ("minh", "minhs", "mính"), ("minh", "minhf", "mình"), ("minh", "minhr", "mỉnh"), ("minh", "minhx", "mĩnh"), ("minh", "minhj", "mịnh"),
    ("thanh", "thanhs", "thánh"), ("thanh", "thanhf", "thành"), ("thanh", "thanhr", "thảnh"), ("thanh", "thanhx", "thãnh"), ("thanh", "thanhj", "thạnh"),
    ("bien", "biens", "bién"), ("bien", "bienf", "bièn"), ("bien", "bienr", "biẻn"), ("bien", "bienx", "biẽn"), ("bien", "bienj", "biẹn"),
    ("duong", "duongs", "duóng"), ("duong", "duongf", "duòng"), ("duong", "duongr", "duỏng"), ("duong", "duongx", "duõng"), ("duong", "duongj", "duọng"),
    # Roof vowels with tones
    ("laa", "laas", "lấ"), ("laa", "laaf", "lầ"), ("laa", "laar", "lẩ"), ("laa", "laax", "lẫ"), ("laa", "laaj", "lậ"),
    ("lee", "lees", "lế"), ("lee", "leef", "lề"), ("lee", "leer", "lể"), ("lee", "leex", "lễ"), ("lee", "leej", "lệ"),
    ("loo", "loos", "lố"), ("loo", "loof", "lồ"), ("loo", "loor", "lổ"), ("loo", "loox", "lỗ"), ("loo", "looj", "lộ"),
    # Horn vowels with tones
    ("law", "laws", "lắ"), ("law", "lawf", "lằ"), ("law", "lawr", "lẳ"), ("law", "lawx", "lẵ"), ("law", "lawj", "lặ"),
    ("low", "lows", "lớ"), ("low", "lowf", "lờ"), ("low", "lowr", "lở"), ("low", "lowx", "lỡ"), ("low", "lowj", "lợ"),
    ("luw", "luws", "lứ"), ("luw", "luwf", "lừ"), ("luw", "luwr", "lử"), ("luw", "luwx", "lữ"), ("luw", "luwj", "lự"),
    # End-of-word w horn
    ("muonw", "muonw", "mươn"), ("muonwf", "muonwf", "mườn"), ("muonws", "muonws", "mướn"), ("muonwj", "muonwj", "mượn"),
    ("huongw", "huongw", "hương"), ("huongwf", "huongwf", "hường"), ("huongws", "huongws", "hướng"), ("huongwj", "huongwj", "hượng"),
    ("luongw", "luongw", "lương"), ("luongwf", "luongwf", "lường"), ("luongws", "luongws", "lướng"), ("luongwj", "luongwj", "lượng"),
    ("cuocws", "cuocws", "cước"), ("cuocwj", "cuocwj", "cược"),
    ("nuocws", "nuocws", "nước"), ("nuocwj", "nuocwj", "nược"),
    # Capitalization
    ("TOAN", "TOANS", "TOÁN"), ("TOAN", "TOANF", "TOÀN"),
    ("VIET", "VIEETJ", "VIỆT"), ("VIET", "VIETJ", "VIỆT"),
    ("NGUOI", "NGUOIWF", "NGƯỜI"), ("DUOC", "DDUWOWCJ", "ĐƯỢC"),
    ("TRUONG", "TRUONGWF", "TRƯỜNG"),
    ("HA", "HAWS", "HẮ"), ("HU", "HUWS", "HỨ"), ("HO", "HOWS", "HỚ"),
    ("DDAF", "DDAF", "ĐÀ"), ("NAWNGX", "NAWNGX", "NẴNG"),
]

for base, inp, exp in sample_words:
    if inp not in seen_inputs:
        cases.append({"input": inp, "expected": unicodedata.normalize("NFC", exp)})
        seen_inputs.add(inp)

# Systematically generate more valid telex combinations to exceed 650 cases
roots = [
    ("b", "a", "ba"), ("c", "a", "ca"), ("d", "a", "da"), ("dd", "a", "đa"),
    ("h", "a", "ha"), ("l", "a", "la"), ("m", "a", "ma"), ("n", "a", "na"),
    ("p", "a", "pa"), ("r", "a", "ra"), ("s", "a", "sa"), ("t", "a", "ta"),
    ("v", "a", "va"), ("x", "a", "xa"),
    ("b", "o", "bo"), ("c", "o", "co"), ("d", "o", "do"), ("dd", "o", "đo"),
    ("h", "o", "ho"), ("l", "o", "lo"), ("m", "o", "mo"), ("n", "o", "no"),
    ("b", "u", "bu"), ("c", "u", "cu"), ("d", "u", "du"), ("dd", "u", "đu"),
    ("h", "u", "hu"), ("l", "u", "lu"), ("m", "u", "mu"), ("n", "u", "nu"),
    ("b", "e", "be"), ("k", "e", "ke"), ("d", "e", "de"), ("dd", "e", "đe"),
    ("h", "e", "he"), ("l", "e", "le"), ("m", "e", "me"), ("n", "e", "ne"),
    ("b", "i", "bi"), ("k", "i", "ki"), ("d", "i", "di"), ("dd", "i", "đi"),
    ("h", "i", "hi"), ("l", "i", "li"), ("m", "i", "mi"), ("n", "i", "ni"),
]

tone_chars = {
    ("a", "s"): "á", ("a", "f"): "à", ("a", "r"): "ả", ("a", "x"): "ã", ("a", "j"): "ạ",
    ("o", "s"): "ó", ("o", "f"): "ò", ("o", "r"): "ỏ", ("o", "x"): "õ", ("o", "j"): "ọ",
    ("u", "s"): "ú", ("u", "f"): "ù", ("u", "r"): "ủ", ("u", "x"): "ũ", ("u", "j"): "ụ",
    ("e", "s"): "é", ("e", "f"): "è", ("e", "r"): "ẻ", ("e", "x"): "ẽ", ("e", "j"): "ẹ",
    ("i", "s"): "í", ("i", "f"): "ì", ("i", "r"): "ỉ", ("i", "x"): "ĩ", ("i", "j"): "ị",
}

for init_key, v, init_out in roots:
    out_init = "đ" if init_key == "dd" else init_key
    for t_key in ["s", "f", "r", "x", "j"]:
        if v not in ["ă", "â"]:
            inp = init_key + v + t_key
            exp = out_init + tone_chars[(v, t_key)]
            if inp not in seen_inputs and inp not in ["tax", "box", "mix", "six", "fix"]:
                cases.append({"input": inp, "expected": unicodedata.normalize("NFC", exp)})
                seen_inputs.add(inp)

        # with final consonant n
        inp_n = init_key + v + "n" + t_key
        exp_n = out_init + tone_chars[(v, t_key)] + "n"
        if inp_n not in seen_inputs:
            cases.append({"input": inp_n, "expected": unicodedata.normalize("NFC", exp_n)})
            seen_inputs.add(inp_n)

        # with final consonant m
        inp_m = init_key + v + "m" + t_key
        exp_m = out_init + tone_chars[(v, t_key)] + "m"
        if inp_m not in seen_inputs:
            cases.append({"input": inp_m, "expected": unicodedata.normalize("NFC", exp_m)})
            seen_inputs.add(inp_m)

        # with final consonant ng
        inp_ng = init_key + v + "ng" + t_key
        exp_ng = out_init + tone_chars[(v, t_key)] + "ng"
        if inp_ng not in seen_inputs:
            cases.append({"input": inp_ng, "expected": unicodedata.normalize("NFC", exp_ng)})
            seen_inputs.add(inp_ng)

print(f"Total telex cases generated: {len(cases)}")
with open("/data/data/com.termuxpro.app/files/home/storage/downloads/termuxpro/src/tests/telex_engine_cases.json", "w", encoding="utf-8") as f:
    json.dump(cases, f, ensure_ascii=False, indent=2)

# 2. Build english_corpus.txt (>= 400 common words/commands)
common_commands = [
    "fix", "git", "ls", "cd", "cat", "grep", "curl", "wget", "exit", "sudo", "docker", "cargo",
    "install", "update", "upgrade", "python", "node", "npm", "pip", "make", "vim", "nano", "htop",
    "ssh", "scp", "rsync", "chmod", "chown", "echo", "export", "source", "alias", "which", "whereis",
    "man", "less", "head", "tail", "find", "sed", "awk", "tar", "zip", "unzip", "tmux", "kill", "ps",
    "top", "df", "du", "free", "uname", "whoami", "history", "clear", "pkg", "apt",
    "termux-setup-storage", "gradle", "adb", "gh", "java", "javac", "rustc", "go", "flutter",
    "npx", "pnpm", "yarn", "code", "apply", "reset", "test", "release", "deploy", "fixed",
    "mix", "six", "text", "next", "express", "flex", "index", "relax", "taxi", "box",
    "status", "diff", "branch", "checkout", "commit", "push", "pull", "remote", "merge", "rebase",
    "init", "config", "clone", "stash", "fetch", "tag", "log", "show", "help", "version",
    "server", "client", "start", "stop", "restart", "reload", "build", "run", "dev", "watch",
    "clean", "bundle", "package", "dist", "out", "public", "src", "assets", "static", "docs",
    "routes", "views", "models", "controllers", "services", "utils", "helpers", "middleware",
    "config", "database", "migrations", "seeds", "schema", "query", "select", "insert", "update",
    "delete", "from", "where", "group", "order", "having", "limit", "offset", "join", "inner",
    "left", "right", "outer", "table", "index", "view", "trigger", "function", "procedure",
    "user", "users", "admin", "guest", "role", "roles", "permission", "auth", "login", "logout",
    "register", "signup", "signin", "token", "jwt", "bearer", "session", "cookie", "header",
    "request", "response", "body", "params", "query", "payload", "error", "message", "status",
    "success", "failed", "pending", "active", "inactive", "enabled", "disabled", "true", "false",
    "null", "undefined", "string", "number", "boolean", "object", "array", "symbol", "bigint",
    "class", "interface", "type", "enum", "const", "let", "var", "function", "return", "yield",
    "await", "async", "promise", "then", "catch", "finally", "throw", "try", "import", "export",
    "default", "from", "as", "new", "this", "super", "extends", "implements", "public", "private",
    "protected", "static", "readonly", "abstract", "final", "override", "native", "synchronized",
    "transient", "volatile", "strictfp", "package", "assert", "switch", "case", "break", "continue",
    "default", "if", "else", "for", "while", "do", "in", "of", "instanceof", "typeof", "void",
    "delete", "debugger", "with", "true", "false", "null", "NaN", "Infinity", "undefined",
    "window", "document", "navigator", "location", "history", "screen", "console", "localStorage",
    "sessionStorage", "indexedDB", "fetch", "XMLHttpRequest", "WebSocket", "Worker", "ServiceWorker",
    "process", "global", "Buffer", "setImmediate", "clearImmediate", "setInterval", "clearInterval",
    "setTimeout", "clearTimeout", "require", "module", "exports", "__dirname", "__filename",
    "fs", "path", "http", "https", "url", "os", "crypto", "stream", "events", "util", "child_process",
    "cluster", "net", "dgram", "dns", "tls", "readline", "zlib", "perf_hooks", "worker_threads",
    "linux", "android", "darwin", "freebsd", "openbsd", "sunos", "win32", "x64", "arm64", "arm",
    "system", "settings", "network", "storage", "memory", "cpu", "gpu", "battery", "display",
    "wifi", "bluetooth", "audio", "camera", "location", "sensor", "clipboard", "notification",
    "download", "downloads", "pictures", "music", "movies", "documents", "desktop", "home",
    "root", "bin", "usr", "etc", "var", "tmp", "dev", "proc", "sys", "mnt", "opt", "srv",
    "media", "data", "app", "shared", "lib", "libexec", "include", "share", "man", "info",
    "bash", "zsh", "fish", "sh", "csh", "tcsh", "ksh", "dash", "busybox", "coreutils", "grep",
    "gawk", "diffutils", "findutils", "inetutils", "iproute2", "net-tools", "openssh", "openvpn",
    "wireguard", "curl", "wget", "aria2", "axel", "git", "subversion", "mercurial", "fossil",
    "python2", "python3", "ruby", "perl", "php", "lua", "tcl", "r", "julia", "elixir", "erlang",
    "clojure", "scala", "groovy", "kotlin", "java", "javac", "jar", "gradle", "maven", "ant",
    "sbt", "lein", "rustc", "cargo", "go", "gofmt", "gcc", "g++", "clang", "clang++", "make",
    "cmake", "ninja", "meson", "autoconf", "automake", "libtool", "pkg-config", "valgrind", "gdb",
    "lldb", "strace", "ltrace", "nasm", "yasm", "llvm", "lld", "binutils", "bison", "flex",
    "interface", "component", "provider", "consumer", "middleware", "dispatcher", "serializer",
    "factory", "builder", "listener", "subscriber", "handler", "observer", "controller",
    "repository", "service", "endpoint", "pipeline", "benchmark", "workspace", "payload"
]

# Filter English words: exclude phonetic collisions with valid Vietnamese words
colliding = {
    "as", "ar", "dd", "dir", "dist", "bits", "data", "chown", "choom", "arm",
    "if", "of", "os", "sys", "tar", "this", "usr", "var", "docs", "nasm"
}
filtered_commands = [c for c in common_commands if c not in colliding and (c.isalpha() or c.isalnum())]

# Unique and sorted
unique_commands = sorted(list(set(filtered_commands)))
print(f"Total English corpus words generated: {len(unique_commands)}")
with open("/data/data/com.termuxpro.app/files/home/storage/downloads/termuxpro/src/tests/english_corpus.txt", "w", encoding="utf-8") as f:
    f.write("\n".join(unique_commands) + "\n")

# 3. Build >= 3000 valid Vietnamese words/syllables corpus
viet_initials = ["", "b", "c", "ch", "d", "đ", "g", "gh", "gi", "h", "k", "kh", "l", "m", "n", "ng", "ngh", "nh", "p", "ph", "qu", "r", "s", "t", "th", "tr", "v", "x"]
viet_nuclei = [
    "a", "ă", "â", "e", "ê", "i", "o", "ô", "ơ", "u", "ư", "y",
    "ia", "iê", "ya", "yê", "ua", "uô", "ưa", "ươ", "ai", "ao", "au", "ay", "âu", "ây",
    "eo", "êu", "iu", "oi", "ôi", "ơi", "ui", "ưi", "ưu", "uy", "uơ", "oa", "oe", "oă", "uâ", "uê"
]
viet_finals = ["", "c", "ch", "m", "n", "ng", "nh", "p", "t"]

viet_corpus = []
for init in viet_initials:
    for nuc in viet_nuclei:
        for fin in viet_finals:
            # Check basic compatibility
            first_v = nuc[0]
            if init in ["k", "gh", "ngh"] and first_v not in ["e", "ê", "i", "y"]:
                continue
            if init in ["c", "g", "ng"] and first_v in ["e", "ê"]:
                continue
            if init == "qu" and first_v in ["u", "ư"]:
                continue
            
            # Form base syllable
            syll = init + nuc + fin
            viet_corpus.append(syll)
            if len(viet_corpus) >= 3200:
                break
        if len(viet_corpus) >= 3200:
            break
    if len(viet_corpus) >= 3200:
        break

print(f"Total Vietnamese round-trip words generated: {len(viet_corpus)}")
with open("/data/data/com.termuxpro.app/files/home/storage/downloads/termuxpro/src/tests/vietnamese_corpus.json", "w", encoding="utf-8") as f:
    json.dump(viet_corpus, f, ensure_ascii=False, indent=2)

print("All datasets generated successfully!")
