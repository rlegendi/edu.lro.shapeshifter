README
======

Introduction
------------
Shapeshifter is a minimal chatbot written in Java, using only statistical
analysis (Nth-order Markov chains, [1]) to create sentences on its own.

It uses the Eclipse Communication Framework to connect to IRC channels, but it
also has an interactive shell that doesn't need any active internet connections
to test its capabilities.

Although it was pretty fun to code this program, it was only a 2-day hack, so
don't expect too much, okay? ;-)

Why another Markov-bot? Aren't there thousands of them on the web already?
--------------------------------------------------------------------------
Yes, there are dozens of them already. As I mentioned above, I coded it just for
fun.

I believe that the best way to understand how something works is to code it on
my own. That's the major reason I created this bot.

Requirements
------------
The program was developed in Eclipse 3.4 (Ganymede), with the help of ECF
version 2.0.0 (v20080623-0046).

Probably you got the full exported project archive with the source and this TXT.
To compile it on your own you need these tools:

- Eclipse Ganymede
	Any build is perfect, I'd recommend the 'Classic' version. They differ only
	in the plugins bundled into them but you're able to install other components
	later on.
	
- The Eclipse Communication Framework Application
	(additional plugin)
	
- The Eclipse Communication Framework Core API
	(additional plugin)

You can download these tools at [0].

Why did you choose the Eclipse Platform?
----------------------------------------
I'm sort of a Java developer and I use Eclipse as a primary IDE. I've it opened
up in the background all the time, spending half of my life in front of it.
Hence it was a natural choice to code a minimal bot with its features if the API
supports my needs - and it did.

I didn't need to implement any boilerplate code, the code is straightforward and
most of the settings can be configured with editing some properties/xml files in
fancy-looking editors.

Running the application
-----------------------
If you're inexperienced in plugin development, I'd recommend reading the
tutorial [7] to get the basic ideas how the compilation and running works.

To be honest, you'll quickly find out that Markov methods are only as good as
their input corpus is. Input a bunch of the same words, or random gibberish, and
that's what you'll get back by the bot.

So, in order to evade gibberish output, we need huge amount of TXT input, and
to make it possible to parse the whole input we should raise the default memory
available to the JVM. Go to the run configuration's JVM arguments and use the
following settings:
 
	-Xms128m -Xmx512m

This was enough for me to make the program parse even a 4-th order Markov model
from a ~180k IRC log (it was about 8M) in ~10 seconds.

When I talk about huge amount, I *mean* huge amount. It could vary depending on
the language of the text, but for Hungarian I copypasted a full ebook of 170
pages and the results haven't satisfied me...

Please note this doesn't represents the difficulty of the languages. It just
means that there're more common word-tuples in Englis than in Hungarian (like
"there is a...", "you are an...", "have you been...", etc.). Markov chains work
with possibly greater success for languages like English.

After you've started the application, join the room specified in the
chatRoomRobot's connectId (by default its at irc://irc.freenode.net/#robotest),
and use the reply command to communicate with the bot:

Example 0:
----------

	[16:21] * Joins: sshifter (n=sshifter@91.82.70.223)
	[16:25] <@roante> ~reinit file:/C:/test.txt
	[16:25] <sshifter2> Started parsing specified file as TXT...
	[16:25] <sshifter2> Engine reinitialization of /C:/test.txt performed [TXT],
	took 1578 msecs. A sum of 78965 tuples were created.
	[16:27] <@roante> ~reply hi bot, how's it goin?
	[16:27] <sshifter> let's see what's in the United States allow chickens as pets,
	the practice is clearly outlined in the universe.
	[16:29] <@roante> ~reply u liek chickens??
	[16:29] <sshifter> only u could do such terrible things.
	[16:29] <@roante> lol
	[16:30] <@roante> ~reply I'm gonna kill you for this >:-)
	[16:30] <sshifter> whoa maxie you look a little horny :D.
	[16:30] <@roante> ~die
	[16:30] <sshifter> Buh-bye! I'm gonna die soon...
	[16:31] <@roante> good night sweet prince!
	[16:31] * sshifter was kicked by roante (roante)

How does it works?
------------------
The engine contains nothing special, mostly the basic stuff required to create
a model. The basic idea is to use Nth order Markov chains to spot common
repetitive patterns in the given text. To get an inner sight on how does Markov
chains work in linguistics and natural language processing, please take a look
at either [2], [3] or [5]. You can find some interesting results of its usage at
[6].

When an input is given, the Loader class parses it either as an IRC log or as a
simple TXT file. For an IRC log a whole line is considered as a sentence (make
sure you removed the system events, time stamps and other unnecessary items from
the log), for a TXT file a line is determined by punctuation characters (any of
the .?! characters).

When we've parsed a sentence, we pass it to the Engine to examine it. It creates
tuples containing exactly N words (N is the parameter of the order of the Markov
chain).

- The lower the N is, the worse the result is . When N==1 the method
  practically concatenates random words after each other - not too useful, eh?

- The higher the N is, the better the result is. Although, for very large N
  values the system doesn't reproduce anything new on its own. You'll be
  facing the same input sentences - still not too useful, right?

Try to leave the N around 3-4, these values reproduce somewhat optimal output.
But, of course, the bot is still going to be pretty dumb :-) 

So, the sentence is splitted to components (using whitespace characters as
delimeters), and we form several N-tuples. This method enables learning smylies
as well :-)

What we examine is the common patterns in the text. We store the words following
and preceding the recognized tuples. If there's no preceding word, we mark the
tuple as 'starter' (it can be used to initiate a new sentence), and if there's
no following word, we mark the tuple as 'finisher' (it can be used to close a
generated sentence).

Example 1
---------
Let's say, we get the input of "Hello bot, I am a magical staff."
As we parse the string, the following tuples are created (consider we're
using a 3rd order Markov model):
	
| Tuple                 | prev word | next word | starter?  | finisher? |
| ----------------------|-----------|-----------|-----------|-----------|
| { Hello, bot, I }     | -         | am        |     X     |    -      |
| { bot, I, am }        | Hello     | a         |     -     |    -      |
| { I, am, a }          | bot       | magical   |     -     |    -      |
| { am, a, magical}     | I         | staff     |     -     |    -      |
| { a, magical, staff } | am        | -         |     -     |    X      |

Easy, isn't it?

My implementation doesn't operates with graphs. When using Markov chains in this
context, they can be easily specified to simple hash relations (containing
Tuples x String pairs). My code not differs too much from the JMegaHal
implementation ([4]), I've just made it more comfortable for myself (no, my code
is not better, I just like do some things other way).

After we built up these structures, the algorithm works this way:

Step 1
------
We read the user's input. We get a random segment of it, and check if it's a
known word for us. We get one of the tuples containing this word.

If there're no tuples (we haven't seen this word yet) there are two options:

1. We simply choose a random tuple to generate a sentence. Not too funny.
2. If we're using Hegedus-heuristics (`[*]`), which is enabled by default, the
   bot simply counter-questions it. That can be used to get some info about the
   word (the bot constantly learns from the replies). Simple but funny stuff ;]

Example 2 
----------
The bot is filled up with Haikus. No chance it knows about vampires.

	[15:01] <@roante> ~reply hey bot, what do you know about antediluvian vampires?
	[15:02] <sshifter> antediluvian?

Step 2
------
We create some sentences using the selected tuple. We examine what words can
follow the specified tuple. We choose one word randomly, and after that we shift
the tuple and perform the examination again. We keep up appending new words to
the sentence until the shifted tuple is a *finisher* one.

When we finish we perform the same task to prepend some words to the sentence
(until we find a tuple that is a *starter*).

Example 3
---------
	[15:05] <@roante> ~reply i hate u bot
	[15:05] <sshifter> i see what u did thar

Here's how the bot created this sentence, using a 2nd order Markov model (sorry
for the *l33tness*, that's the curse of IRC, I had nothing but an IRC log at hand):

	>> Trying to reply to word: u
	>> Selected tuple: [what, u]

	------------------------------------- Building postfix
	>> Available next tokens: [did, are]
	>> Chosen one is: did
	>> Sentence: what u did
	>> Shifted tuple is: [u, did]

	>> Available next tokens: [thar, with, mother]
	>> Chosen one is: thar
	>> Sentence: what u did thar
	>> Shifted tuple is: [did, thar] (finisher)

	>> Finished!

	------------------------------------- Building prefix
	>> Available prev tokens: [see]
	>> Chosen one is: see
	>> Sentence: see what u did thar
	>> Shifted tuple is: [see, u]

	>> Available prev tokens: [i, noone, will]
	>> Chosen one is: i
	>> Sentence: i see what u did thar
	>> Shifted tuple is: [i, see] (starter)

	>> Finished!

	[28]	i see what u did thar
	=============================

*[The number '28' before the sentece is the Strack-entropy value of it, that we
discuss in the next step]*

Step 3
------
We create some sentences, and display the one that has the greatest information.
I refer to this value as *entropy*.

I've decided to have 2 natural requirements against the generated sentences,
these are the following ones:

1. I like longer sentences - the longer the better.
2. I don't want to see sentences identical to one of the input sentences.

To satisfy these requirements, I add some entropy points to the sentence after
each word (longer sentences going to have higher values), and after each choice
made we also assign some bonus value to the sentence's entropy value, since if a
choice is made between candidates which to put into the sentence it means the
bot made something on its own, this is the so-called Strack-entropy (`[**]`).

Step 4
------
Use the user's input to update the Markov model, so that the bot can learn from
what the user types.

--------------------------------------------------------------------------------

My Experiences
--------------
To be honest, I thought this whole stuff would work better. I've tried to
maximize its input to the limits, but haven't got any valuable output. But at
least it has no specific dependencies, so can be used with text written in any
language.

Btw, if you find any bugs or has any comments with the program, please feel free
to contact me at richard.legendi@gmail.com.

The source is yours, you can freely modify and play with it. Have fun!

--------------------------------------------------------------------------------

Acknowledgement
---------------
Friends helped a little to play with the bot:
daanika, ribes, pedroo

and of course the #eclipse IRC channel for their constant support ;-)

<irc://irc.freenode.net/#eclipse>

Special thx to rcjsuen for making KOS-MOS' source available for everyone who's
interested in it!

--------------------------------------------------------------------------------

References
----------

[0]		Eclipse Downloads
		http://www.eclipse.org/downloads/

[1]		Markov chains
		http://en.wikipedia.org/wiki/Markov_chain
		
[2]		Lawrence Kesteloot's article about interpolating between two
		basically different Markov chains
		http://www.teamten.com/lawrence/projects/markov/
		
[3]		Jason Hutchens: How megahal works
		http://megahal.alioth.debian.org/How.html
		
[4]		JMegaHal: a Java implementation of the Megahal engine by Paul Mutton
		(he's the guy who wrote the IRC Hacks book)
		http://www.jibble.org/jmegahal/

[5]		The Markov chatbot written by Carlo Teubner
		http://www.cteu.de/markov/
		
[6]		Jeff Atwood's article about Garkov (when Garfield meets Markov chains
		lol). Unfortunately, their main site seems dead for a while so that's
		all I found about it on the web :-(
		http://www.codinghorror.com/blog/archives/001132.html

[7]		Remy Chi Jian Suen, Nick Boldt, Scott Lewis: The ECF Bot Framework
		http://wiki.eclipse.org/Bot_Framework
		
[8]		Some fun between the reading of the other references to the end ;-)
		http://www.blastwavecomic.com/
		
`[*]`		These stuff are non-existing names, so don't be bothered if you find
`[**]`	nothing about them in google :-) Some of my friends mentioned above
		suggested a few ideas how to improve the bot. So I named the mechanics,
		variables after them. Hope they don't mind :P

--------------------------------------------------------------------------------

Appendix
========

Bot commands
------------
Currently the following commands are supported by the bot:

	die, help, hh, list, order, re, reinit, reply, se.

To make the bot react to your command, append a `~` (tilde) prefix to it:

Example 4
---------
	[15:44] <@roante> ~list
	[15:44] <sshifter> Available commands: die, help, hh, list, order, re, reinit,
	reply, se.

~die:
-----
Usage: `~die order`
Kills the bot immediately

~help
-----
Usage: `~help [command]`
Shows usage info for the given command (type `~list` for the list of commands).
The `[...]` notation is used for optional parameters.

~hh
---
Usage: `~hh`
Turns using Hegedus-heuristics on/off.

~list
-----
Usage: `~list`
Lists the available commands.

~order
------
Usage: `~order [order]`
Sets the order of the used Markov-chains. Order must be an integer value between
[1,5]. Automatically reinits the knowledge base. If no order was specified,
prints the current order setting.

~re
---
Usage: `~re`
About info.

~reinit
-------
Usage: `~reinit [URL]`
Reinitilaizes the knowledge of the bot with the default knowledge base. If an
URL is specified, it tries to connect the specified source and learns from its
content. It is hardly encouraged to use simple txt files.

Example 5
---------
Reinit with a simple TXT file on the web:

	[16:00] <@roante> ~reinit http://roante.dyndns.org/test.txt
	[16:00] <sshifter> Started parsing specified file as TXT...
	[16:00] <sshifter> Engine reinitialization performed [TXT], took 12234 msecs. A
	sum of 446682 tuples were created.

Reinit with an IRC log on the web (please note there should be 1 sentence in 1
line without any system messages, timestamps, etc., or the output would be still
influenced by them):

	[16:02] <@roante> ~reinit http://roante.dyndns.org/test.log IRC_LOG
	[16:02] <sshifter> Started parsing specified file as IRC_LOG...
	[16:02] <sshifter> Engine reinitialization performed [IRC_LOG], took 107 msecs.
	A sum of 1213 tuples were created.

Reinit with a simple TXT file on the host computer:

	[16:02] <@roante> ~reinit file:/c:/test.txt
	[16:02] <sshifter> Started parsing specified file as TXT...
	[16:02] <sshifter> Engine reinitialization performed [TXT], took 11359 msecs. A
	sum of 567302 tuples were created.

~reply
------
Usage: `~reply [question]`
Used to generate a new sentence. If a parameter is specified, the bot tries to
respond for it.

~se
---
Usage: `~se`
Sets the Strack-entrophy compensation value (by default it's `7`).


--------------------------------------------------------------------------------

Tips
----
# When you're reinitializing the bot, try to edit the document before to improve
  the bot's output. Remove all `"` characters, try to remove unncessary punctuations
  (like the dot from Elizabeth I. of England).
# When asking from the bot, try to ignore punctuation characters. When you
  simply ask the bot with `"~reply me?"` the only choice the bot has is to search
  for the `"me?"` string in its brain - what is possibly don't exist, and the bot
  is going to produce some random reply.

--------------------------------------------------------------------------------

Richard O. Legendi
richard.legendi@gmail.com
aug. 07., 2008

If you got questions, don't hesitate to write, maybe I have some of the
answers - at least I can give you some links :-)

--------------------------------------------------------------------------------

