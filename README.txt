Pingtool usage:

In der Shell (also einfach im Kommandofenster) sind folgende Befehle möglich:

 - exit
 - quit
 - ping 123.123.123.123

Das Kommando ping versendet 4 ICMP Echo Requests an die angegebene IP und wartet
auf eine Antwort vom Zielhost. Das Timeout ist dabei auf eine Sekunde gestellt,
wird nach diesem Timeout kein Echo Reply empfangen, so wird eine Fehlermeldung
ausgegeben. Treffen diese Pakete später doch noch ein, wird das im Kommando-
fenster ausgegeben. Bei einem empfangenen Echo Reply wird überprüft, ob der
Header stimmt (Quell-IP, Identifier, Sequence Nr.) und ob die Payload mit der
des Echo Requests übereinstimmt.
Wird keine IP oder eine ungültige IP eingegeben, spuckt das Programm eine
Fehlermeldung aus.

