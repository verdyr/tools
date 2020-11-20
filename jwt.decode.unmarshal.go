package main

import (
        "encoding/json"
        "gopkg.in/square/go-jose.v2"
        "log"
)

func main() {
        x := "{\"keys\":[{\"kid\":\"1Hqp05I7Y2Nn4CtijrtEfsBjBHtVG00sRzfle9Kyv5k\",\"kty\":\"EC\",\"alg\":\"ES256\",\"use\":\"sig\",\"crv\":\"P-256\",\"x\":\"AK5SyUxGM8pfoDwDc_9HP4el5zzoh5AXw7lCekEKVHIG\",\"y\":\"bNTXMIvfpEvkA4AFar7cQ-WgRYuL_60ZlQDB6NWsk7Q\"},{\"kid\":\"o-TY8hyG8njU4tIRk0rIN2hfCTP16yzw2qehVU0A1xk\",\"kty\":\"RSA\",\"alg\":\"RS256\",\"use\":\"sig\",\"n\":\"j6bfgm4K2Ak4xqV8SC3BlSr5HnUj1bCLmBfBqms8w43pndt4FSkB5z60aAz_KDJ46C8wTyTHYNPwtvxJDgzQDxEGhCvRbeuno2v8ARCbGHLDeX8_5RGxqCu-rOxb7y2K8gcaMtC2iIG9BN_DMzsfXE35wqMNl6ifZTU9z2ZErJKBgpXV7b32AV64EyvCWUZFIJGwjkj4QB1hmdoZpB97u1CfC8lS5QmWEB8vuXo2YY35Pu2zt5VrHnpNi_Ejy07t-Ly7gaNKUQBkoZn0p_lFyJHXg2nMc_aBQ5nX50TtViKGqJFSKEOjmQP8vmIZyWWtK7vBnmcXhofOMArJ4p4pBQ\",\"e\":\"AQAB\"}]}"

        var into jose.JSONWebKeySet
        err := json.Unmarshal([]byte(x), &into)
        if err != nil {
                log.Println("Failed due to error, ", err)
        }
}
