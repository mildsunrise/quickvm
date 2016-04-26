# quickvm

quickvm is a little tool that interprets Smali disassembly,
like a real Dalvik VM would.

The main motivation to write this was that some obfuscators
*encrypt string constants*, putting code at the static constructor
that decrypts them and stores them into static fields. Here's an
example of a disassembled class with an encrypted string:

~~~ smali
...

# static fields
.field private static final z:Ljava/lang/String;

# direct methods
.method static constructor <clinit>()V
    .locals 5

    const-string/jumbo v0, "\u0011\u00172x\u00031\u001c{f\u0001\u007f\u0008}{\u0012\u007f\u0019di\u000f3\u0019pd\u0003"
    invoke-virtual {v0}, Ljava/lang/String;->toCharArray()[C
    move-result-object v0
    array-length v1, v0
    const/4 v2, 0x0
    move v3, v2
    move v2, v1
    move-object v1, v0

    :goto_0
    if-gt v2, v3, :cond_0
    new-instance v0, Ljava/lang/String;
    invoke-direct {v0, v1}, Ljava/lang/String;-><init>([C)V
    invoke-virtual {v0}, Ljava/lang/String;->intern()Ljava/lang/String;
    move-result-object v0
    sput-object v0, Lde/greenrobot/event/d;->z:Ljava/lang/String;
    return-void

    :cond_0
    aget-char v4, v1, v3
    rem-int/lit8 v0, v3, 0x5
    packed-switch v0, :pswitch_data_0
    const/16 v0, 0x66

    :goto_1
    xor-int/2addr v0, v4
    int-to-char v0, v0
    aput-char v0, v1, v3
    add-int/lit8 v0, v3, 0x1
    move v3, v0
    goto :goto_0

    :pswitch_0
    const/16 v0, 0x5f
    goto :goto_1

    :pswitch_1
    const/16 v0, 0x78
    goto :goto_1

    :pswitch_2
    const/16 v0, 0x12
    goto :goto_1

    :pswitch_3
    const/16 v0, 0x8
    goto :goto_1

    nop

    :pswitch_data_0
    .packed-switch 0x0
        :pswitch_0
        :pswitch_1
        :pswitch_2
        :pswitch_3
    .end packed-switch
.end method

...
~~~

quickvm's will interpret statements in the smali file, simulating a VM
executing the static constructor, and tell you the values of static fields
as the code initializes them. For the above class, quickvm would report:

~~~
Trying file: de/greenrobot/event/d.smali
Lde/greenrobot/event/d;->z:Ljava/lang/String; = "No pending post available"
~~~

A nice advantage of this method is that it doesn't depend on any obfuscator
in particular. It will decrypt them as long as the decryption takes place
from the static constructor (but see limitations below).

quickvm can work with (and print) all primitive values (booleans, chars and
integers), plus strings, arrays and some common containers like `ArrayList`.
Booleans and chars are printed as integrers. Example:

~~~
Lde/greenrobot/event/util/h;->b:Z = 1
Lde/greenrobot/event/d;->z:Ljava/lang/String; = "No pending post available"
Lde/greenrobot/event/m;->z:[Ljava/lang/String; = [ "PostThread", "BackgroundThread", "MainThread", "Async" ]
Lde/greenrobot/event/util/h;->a:I = 0
Lorg/whispersystems/O;->a:[B = [ 1, 59, 3 ]
Lorg/whispersystems/bF;->a:I = 16777215
~~~

So, even when the constants are not encrypted, it's still handy to have them
in readable form, instead of having to read the static constructor.


## Usage

Build the code (running `ant jar` should suffice) and then change to the root
of the smali disassembly (this root will usually contain `com/`, `org/`, et all).
From there:

    find | grep '\.smali$' | java -jar ~/path/to/quickvm/dist/quickvm.jar . > constants.txt

And watch quickvm try to load each of your classes, and probably fail on
most of them. Constants will be saved to `constants.txt`.

If quickvm fails on most classes, it's probably because of some
instruction or API call that the obfuscator likes to use and is not implemented.
It should be implemented in `Ops.java` or `Methods.java` respectively.

If most classes complete without errors (quickvm just moves to the next one)
then good news! Even if some classes still fail, decryption is usually performed
right at the beginning of the static constructor, so by the time those classes
fail (see limitations below), their constants have probably been decrypted
and printed already. However, reviewing those classes manually is still a good idea...

### Big codebases

When running this at big codebases (500+ classes), it's a good idea to skip loading
classes you don't care about, such as support library, google, etc. It's also better
to log the error stream as well, so we can review the errors later.

    find com/provider1 | grep '\.smali$' | java -jar ~/path/to/quickvm/dist/quickvm.jar . > constants.txt 2> status.txt


## Limitations

Although a useful tool, it's still very limited for a VM:

 - No notion of type inheritance, so type casts, method calls, etc.
   can not be implemented with the current design.

 - The VM does not have exceptions, and implementing them would require
   significant refactoring and such.

 - No STL, only a few methods implemented in `Methods.java`. If the Smali calls
   any other method of the STL, you'll have to implement it too.

 - Slow as fuck. Which is expected, knowing this "VM" runs off Smali code
   instead of actual bytecode, doesn't have a GC, isn't written with efficiency
   in mind, and was coded in a few hours.

 - A lot of opcodes are not implemented yet. 64-bit operations, floating
   arithmetic, branching... Again, I hacked this up for my needs rather than
   as a serious project.

In a way, some of these limitations were actually *intended*, because the aim
of this tool is to extract static constants. By not implementing I/O, threads,
or other fancy features, we can be sure that constants extracted by quickvm
do not depend on the environment in any way. This is the fundamental difference
between using this tool and making a memory dump of the Dalvik VM while the app
is loaded.

Issues and improvements are welcome. Keep in mind this is just a tool, and it's
especially crappy. It can (and will) miss interesting stuff, so take the results
with a pinch of salt. It's a good idea to make sure to only run this at Smali
code coming straight from the disassembler, and make sure the assembled code
runs on a real Android VM without crashing.


