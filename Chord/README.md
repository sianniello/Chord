# Chord Project

Il software crea una rete p2p di nodi che permettono di salvare file utilizzando una DHT.

I package le classi principali:
 - JoinServer
 - Node
 - ServerHandler
 - Request

### Join Server
Conosce tutti i nodi connessi alla rete e fornisce loro l'elenco degli indirizzi. Periodicamente controlla lo stato dei nodi.

### Node
E' l'elemento costitutivo del ring, contiene l'intera logica applicativa e istanzia i Thread di gestione delle connessioni da un pool predefinito.

### ServerHandler
Elabora le richieste ricevute e può inoltrare messaggi ad altri server. Si occupa del mantenimento della consistenza del ring e della gestione dei file.

### Request
Rappresenta il pacchetto scambiato tra le varie entità della rete. Il pacchetto standard ha struttura (destinatario, richiesta, dati, mittente)

### Avvio
Per prima cosa è necessario avviare il Join server specificando la porta
```sh
Enter Join Server port:
```

A questo punto è possibile scegliere se avviare singolarmente i nodi oppure avviare la classe di simulazione Init. Nel primo caso si dovrà scegliere una porta compresa tra 10000 e 10100 e specificare l'indirizzo (basta solo il numero di porta) del join server.

```sh
Enter node port:
10000
Enter JoinServer IP address(xxx.xxx.xxx.xxx) or its network name:
localhost
Enter join server port:
1099
```

A questo punto dal menu è possibile scegliere le operazione da eseguire. Ovviamente è possibile avviare più nodi contemporaneamente sulla stessa macchina.
Se si avvia l'esecuzione dalla classe di simulazione init si dovrà prima settare l'ambiente modificando direttamente la classe e facendo attenzione a inserire dei ritardi tra un operazione e l'altra per evitare conflitti di temporizzazione.
