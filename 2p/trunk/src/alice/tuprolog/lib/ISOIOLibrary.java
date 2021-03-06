package alice.tuprolog.lib;

/**
 * @author: Sara Sabioni
 */

import static alice.tuprolog.TuPrologError.*;
import static alice.tuprolog.TuFactory.*;
import alice.tuprolog.*;

import java.util.*;
import java.io.*;

/**
 * This class provides basic ISO I/O predicates.
 * 
 * Library/Theory Dependency: IOLibrary
 * 
 * 
 */

public class ISOIOLibrary extends TuLibrary {
    private static final long serialVersionUID = 1L;
    protected final int files = 1000; //numero casuale abbastanza alto per evitare eccezioni sulle dimensioni delle hashtable
    protected Hashtable<InputStream, Hashtable<String, Term>> inputStreams = new Hashtable<InputStream, Hashtable<String, Term>>(
            files);
    protected Hashtable<OutputStream, Hashtable<String, Term>> outputStreams = new Hashtable<OutputStream, Hashtable<String, Term>>(
            files);

    protected InputStream inputStream = null;
    protected OutputStream outputStream = null;
    protected String inputStreamName = null;
    protected String outputStreamName = null;
    protected IOLibrary IOLib = null;

    private int flag = 0;
    private int write_flag = 1;

    public ISOIOLibrary() {

    }

    public boolean open_4(Term source_sink, Term mode, Term stream, Term options) throws TuPrologError {
        initLibrary();
        source_sink = source_sink.dref();
        mode = mode.dref();

        if (source_sink.isVar()) {
            throw instantiation_error(engine.getEngineManager(), 1);
        }

        File file = new File(((Term) source_sink).fname());
        if (!file.exists()) {
            throw existence_error(engine
                    .getEngineManager(), 1, "source_sink", source_sink, createTuAtom("File not found."));
        }

        if (mode.isVar()) {
            throw instantiation_error(engine.getEngineManager(), 2);
        } else if (!mode.isAtomSymbol()) {
            throw type_error(engine.getEngineManager(), 1, "atom", mode);
        }

        if (!(stream.isVar())) {
            throw type_error(engine.getEngineManager(), 3, "variable", stream);
        }

        Hashtable<String, Term> properties = new Hashtable<String, Term>(10);
        boolean result = inizialize_properties(properties);
        BufferedOutputStream output = null;
        BufferedInputStream input = null;

        if (result == true) {
            Term openOptions = (Term) options;
            Term in_out = (Term) source_sink;
            if (openOptions.isPlList()) {
                if (!openOptions.isEmptyList()) {
                    Iterator<? extends Term> i = openOptions.listIteratorProlog();
                    while (i.hasNext()) {
                        Term option = null;
                        Term obj = i.next();
                        if (obj.isVar()) {
                            throw instantiation_error(engine.getEngineManager(), 4);
                        }
                        option = (Term) obj;
                        if (!properties.containsKey(option.fname())) {
                            throw domain_error(engine.getEngineManager(), 4, "stream_option", option);
                        }

                        //controllo che alias non sia gia' associato ad uno stream aperto
                        if (option.fname().equals("alias")) {
                            //ciclo su inputStreams
                            for (Map.Entry<InputStream, Hashtable<String, Term>> currentElement : inputStreams
                                    .entrySet()) {
                                for (Map.Entry<String, Term> currentElement2 : currentElement.getValue().entrySet()) {
                                    if (currentElement2.getKey().equals("alias")) {
                                        Term alias = currentElement2.getValue();
                                        for (int k = 0; k < option.getPlArity(); k++) {
                                            if (((Term) alias).getPlArity() > 1) {
                                                for (int z = 0; z < ((Term) alias).getPlArity(); z++) {
                                                    if ((((Term) alias).getPlainArg(z)).equals(option.getPlainArg(k))) {
                                                        throw permission_error(engine
                                                                .getEngineManager(), "open", "source_sink", alias, createTuAtom("Alias is already associated with an open stream."));
                                                    }
                                                }
                                            } else if (alias.equals(option.getPlainArg(k))) {
                                                throw permission_error(engine
                                                        .getEngineManager(), "open", "source_sink", alias, createTuAtom("Alias is already associated with an open stream."));
                                            }
                                        }
                                    }
                                }
                            }
                            //ciclo su outputStreams (alias deve essere unico in tutti gli stream aperti
                            //sia che siano di input o di output)
                            for (Map.Entry<OutputStream, Hashtable<String, Term>> currentElement : outputStreams
                                    .entrySet()) {
                                for (Map.Entry<String, Term> currentElement2 : currentElement.getValue().entrySet()) {
                                    if (currentElement2.getKey().equals("alias")) {
                                        Term alias = currentElement2.getValue();
                                        for (int k = 0; k < option.getPlArity(); k++) {
                                            if (((Term) alias).getPlArity() > 1) {
                                                for (int z = 0; z < ((Term) alias).getPlArity(); z++) {
                                                    if ((((Term) alias).getPlainArg(z)).equals(option.getPlainArg(k))) {
                                                        throw permission_error(engine
                                                                .getEngineManager(), "open", "source_sink", alias, createTuAtom("Alias is already associated with an open stream."));
                                                    }
                                                }
                                            } else if (alias.equals(option.getPlainArg(k))) {
                                                throw permission_error(engine
                                                        .getEngineManager(), "open", "source_sink", alias, createTuAtom("Alias is already associated with an open stream."));
                                            }
                                        }
                                    }
                                }
                            }
                            int arity = option.getPlArity();
                            if (arity > 1) {
                                Term[] arrayTerm = new Term[arity];
                                for (int k = 0; k < arity; k++) {
                                    arrayTerm[k] = option.getPlainArg(k);
                                }
                                properties.put(option.fname(), createTuStructA(".", arrayTerm));
                            } else {
                                properties.put(option.fname(), option.getPlainArg(0));
                            }
                        } else {
                            Term value = null;
                            value = (Term) option.getPlainArg(0);
                            properties.put(option.fname(), value);
                        }
                    }
                    properties.put("mode", mode);
                    properties.put("file_name", source_sink);
                }
            } else {
                throw type_error(engine.getEngineManager(), 4, "list", openOptions);
            }

            Term structMode = (Term) mode;
            if (structMode.fname().equals("write")) {
                try {
                    output = new BufferedOutputStream(new FileOutputStream(in_out.fname()));
                } catch (Exception e) {
                    //potrebbe essere sia FileNotFoundException sia SecurityException
                    throw permission_error(engine
                            .getEngineManager(), "open", "source_sink", source_sink, createTuAtom("The source_sink specified by Source_sink cannot be opened."));
                }
                properties.put("output", createTuAtom("true"));
                outputStreams.put(output, properties);
                return unify(stream, createTuAtom(output.toString()));
            } else if (structMode.fname().equals("read")) {
                try {
                    input = new BufferedInputStream(new FileInputStream(in_out.fname()));
                } catch (Exception e) {
                    throw permission_error(engine
                            .getEngineManager(), "open", "source_sink", source_sink, createTuAtom("The source_sink specified by Source_sink cannot be opened."));
                }
                properties.put("input", createTuAtom("true"));

                //mi servono queste istruzioni per set_stream_position
                //faccio una mark valida fino alla fine del file, appena lo apro in modo che mi possa
                //permettere di fare una reset all'inizio del file. Il +5 alloca un po di spazio in piu
                //nel buffer, mi serve per per evitare che la mark non sia piu valida quando leggo la fine del file
                if (((Term) properties.get("reposition")).fname().equals("true")) {
                    try {
                        input.mark((input.available()) + 5);
                    } catch (IOException e) {
                        // ED 2013-05-21: added to prevent Java warning "resource leak", input not closed
                        try {
                            input.close();
                        } catch (IOException e2) {
                            throw system_error(TuFactory.createTuAtom("An error has occurred in open when closing the input file."));
                        }
                        // END ED
                        throw system_error(TuFactory.createTuAtom("An error has occurred in open."));
                    }
                }
                inputStreams.put(input, properties);
                return unify(stream, createTuAtom(input.toString()));
            } else if (structMode.fname().equals("append")) {
                try {
                    output = new BufferedOutputStream(new FileOutputStream(in_out.fname(), true));
                } catch (Exception e) {
                    throw permission_error(engine
                            .getEngineManager(), "open", "source_sink", source_sink, createTuAtom("The source_sink specified by Source_sink cannot be opened."));
                }
                properties.put("output", createTuAtom("true"));
                outputStreams.put(output, properties);
                return unify(stream, createTuAtom(output.toString()));
            } else {
                throw domain_error(engine.getEngineManager(), 2, "io_mode", mode);
            }
        } else {
            TuPrologError
                    .system_error(TuFactory.createTuAtom("A problem has occurred with initialization of properties' hashmap."));
            return false;
        }
    }

    public boolean open_3(Term source_sink, Term mode, Term stream) throws TuPrologError {
        initLibrary();

        source_sink = source_sink.dref();
        File file = new File(((Term) source_sink).fname());
        if (!file.exists()) {
            throw existence_error(engine
                    .getEngineManager(), 1, "source_sink", source_sink, createTuAtom("File not found"));
        }
        mode = mode.dref();
        if (source_sink.isVar()) {
            throw instantiation_error(engine.getEngineManager(), 1);
        }

        if (mode.isVar()) {
            throw instantiation_error(engine.getEngineManager(), 2);
        } else if (!mode.isAtomSymbol()) {
            throw type_error(engine.getEngineManager(), 1, "atom", mode);
        }

        if (!(stream.isVar())) {
            throw type_error(engine.getEngineManager(), 3, "variable", stream);
        }

        //siccome ? una open con la lista delle opzioni vuota, inizializzo comunque le opzioni
        //e inoltre inserisco i valori che gi? conosco come file_name,mode,input,output e type.
        Hashtable<String, Term> properties = new Hashtable<String, Term>(10);
        boolean result = inizialize_properties(properties);

        BufferedOutputStream output = null;
        BufferedInputStream input = null;
        Term structMode = (Term) mode;

        if (result == true) {
            Term in_out = (Term) source_sink;
            Term value = createTuAtom(in_out.fname());
            properties.put("file_name", value);
            properties.put("mode", mode);

            if (structMode.fname().equals("write")) {
                try {
                    output = new BufferedOutputStream(new FileOutputStream(in_out.fname()));
                } catch (Exception e) {
                    //potrebbe essere sia FileNotFoundException sia SecurityException
                    throw permission_error(engine
                            .getEngineManager(), "open", "source_sink", source_sink, createTuAtom("The source_sink specified by Source_sink cannot be opened."));
                }
                properties.put("output", createTuAtom("true"));
                outputStreams.put(output, properties);
                return unify(stream, createTuAtom(output.toString()));
            } else if (structMode.fname().equals("read")) {
                try {
                    input = new BufferedInputStream(new FileInputStream(in_out.fname()));
                } catch (Exception e) {
                    throw permission_error(engine
                            .getEngineManager(), "open", "source_sink", source_sink, createTuAtom("The source_sink specified by Source_sink cannot be opened."));
                }
                properties.put("input", createTuAtom("true"));

                //vedi open_4 per spiegazione
                if (((Term) properties.get("reposition")).fname().equals("true")) {
                    try {
                        input.mark((input.available()) + 5);
                    } catch (IOException e) {
                        // ED 2013-05-21: added to prevent Java warning "resource leak", input not closed
                        try {
                            input.close();
                        } catch (IOException e2) {
                            throw system_error(TuFactory.createTuAtom("An error has occurred in open when closing the input file."));
                        }
                        // END ED
                        throw system_error(TuFactory.createTuAtom("An error has occurred in open."));
                    }
                }

                inputStreams.put(input, properties);
                return unify(stream, createTuAtom(input.toString()));
            } else if (structMode.fname().equals("append")) {
                try {
                    output = new BufferedOutputStream(new FileOutputStream(in_out.fname(), true));
                } catch (Exception e) {
                    throw permission_error(engine
                            .getEngineManager(), "open", "source_sink", source_sink, createTuAtom("The source_sink specified by Source_sink cannot be opened."));
                }
                properties.put("output", createTuAtom("true"));
                outputStreams.put(output, properties);
                return unify(stream, createTuAtom(output.toString()));
            } else {
                throw domain_error(engine.getEngineManager(), 1, "stream", in_out);
            }
        } else {
            system_error(TuFactory.createTuAtom("A problem has occurred with the initialization of the hashmap properties."));
            return false;
        }
    }

    public boolean close_2(Term stream_or_alias, Term closeOptions) throws TuPrologError {
        initLibrary();
        //Struct result = null;
        OutputStream out = null;
        InputStream in = null;

        boolean force = false;
        Term closeOption = (Term) closeOptions;

        if (closeOptions.isPlList()) {
            if (!closeOptions.isEmptyList()) {
                Iterator<? extends Term> i = closeOption.listIteratorProlog();
                while (i.hasNext()) {
                    Term option = null;
                    Term obj = i.next();
                    if (obj.isVar()) {
                        throw instantiation_error(engine.getEngineManager(), 4);
                    }
                    option = (Term) obj;
                    if (option.fname().equals("force")) {
                        Term closeOptionValue = (Term) option.getPlainArg(0);
                        force = closeOptionValue.fname().equals("true") ? true : false;
                    } else {
                        throw domain_error(engine.getEngineManager(), 2, "close_option", option);
                    }
                }
            }
        } else {
            throw type_error(engine.getEngineManager(), 4, "list", closeOptions);
        }

        //Siccome non so di quale natura ? lo stream, provo a cercarlo sia in inputStreams che
        //in outputStreams se in inputStreams non c'? la funzione lancia un errore
        //raccolgo l'eccezione e controllo in out. Se anche l? non c'? non raccolgo l'eccezione
        //perch? significa che lo stream che mi ? stato passato non ? aperto.
        try {
            in = find_input_stream(stream_or_alias);
        } catch (TuPrologError p) {
            out = find_output_stream(stream_or_alias);
        }

        if (out != null) {
            String out_name = get_output_name(out);
            if (out_name.equals("stdout")) {
                return true;
            }
            try {
                flush_output_1(stream_or_alias);
                out.close();
            } catch (IOException e) {
                if (force == true) {//devo forzare la chiusura 
                    //siccome in java non c'? modo di forzare la chiusura ho modellato il problema
                    //eliminando ogni riferimento all'oggetto stream, in modo tale che venga eliminato dal
                    //dal garabage colletor.
                    outputStreams.remove(in);
                    out = null;
                    //nel caso in cui lo stream che viene chiuso ? lo stream corrente, riassegno stdin o stdout
                    //ai riferimenti dello stream corrente
                    if (out_name.equals(outputStreamName)) {
                        outputStreamName = "stdout";
                        outputStream = System.out;
                    }
                } else {//lo stream rimane aperto,avverto che si sono verificati errori
                    throw system_error(TuFactory.createTuAtom("An error has occurred on stream closure."));
                }
            }
        } else if (in != null) {
            String in_name = get_input_name(in);
            if (in_name.equals("stdin")) {
                return true;
            }
            try {
                in.close();
            } catch (IOException e) {
                if (force == true) {
                    inputStreams.remove(in);
                    in = null;
                    if (in_name.equals(inputStreamName)) {
                        inputStreamName = "stdin";
                        inputStream = System.in;
                    }
                } else {
                    throw system_error(TuFactory.createTuAtom("An error has occurred on stream closure."));
                }
            }
            inputStreams.remove(in);
        }
        return true;
    }

    public boolean close_1(Term stream_or_alias) throws TuPrologError {
        initLibrary();
        //Struct result = null;
        OutputStream out = null;
        InputStream in = null;

        try {
            in = find_input_stream(stream_or_alias);
        } catch (TuPrologError p) {
            out = find_output_stream(stream_or_alias);
        }

        if (out != null) {
            String out_name = get_output_name(out);
            if (out_name.equals("stdout")) {
                return true;
            }
            flush_output_1(stream_or_alias);
            try {
                out.close();
            } catch (IOException e) {
                throw system_error(TuFactory.createTuAtom("An error has occurred on stream closure."));
            }
            if (out_name.equals(outputStreamName)) {
                outputStreamName = "stdout";
                outputStream = System.out;
            }
            outputStreams.remove(out);
        } else if (in != null) {
            String in_name = get_input_name(in);
            if (in_name.equals("stdin")) {
                return true;
            }
            try {
                in.close();
            } catch (IOException e) {
                throw system_error(TuFactory.createTuAtom("An error has occurred on stream closure."));
            }
            if (in_name.equals(inputStreamName)) {
                inputStreamName = "stdin";
                inputStream = System.in;
            }
            inputStreams.remove(in);
        }
        return true;
    }

    public boolean set_input_1(Term stream_or_alias) throws TuPrologError {
        initLibrary();
        InputStream stream = find_input_stream(stream_or_alias);
        Hashtable<String, Term> entry = inputStreams.get(stream);
        Term name = (Term) entry.get("file_name");
        inputStream = stream;
        inputStreamName = name.fname();
        return true;
    }

    public boolean set_output_1(Term stream_or_alias) throws TuPrologError {
        initLibrary();
        OutputStream stream = find_output_stream(stream_or_alias);
        Hashtable<String, Term> entry = outputStreams.get(stream);
        Term name = (Term) entry.get("file_name");
        outputStream = stream;
        outputStreamName = name.fname();
        return true;
    }

    //funzione integrata con codice Prolog: ricerca, data una proprieta', tutti gli stream
    //che la soddisfano
    public boolean find_property_2(Term list, Term property) throws TuPrologError {
        initLibrary();
        if (outputStreams.isEmpty() && inputStreams.isEmpty()) {
            return false;
        }

        if (!(list.isVar())) {
            throw instantiation_error(engine.getEngineManager(), 1);
        }

        property = property.dref();
        Term prop = (Term) property;
        String propertyName = prop.fname();
        Term propertyValue = null;
        if (!propertyName.equals("input") && !propertyName.equals("output")) {
            propertyValue = (Term) prop.getPlainArg(0);
        }
        List<Term> resultList = new ArrayList(); //object generico perche' sono sia inputStream che outputStream

        if (propertyName.equals("input")) {
            for (Map.Entry<InputStream, Hashtable<String, Term>> stream : inputStreams.entrySet()) {
                resultList.add(TuFactory.createTuAtom(stream.getKey().toString()));
            }
            Term result = createTuListStruct(resultList.toArray(new Term[1]));
            return unify(list, result);
        } else if (propertyName.equals("output")) {
            for (Map.Entry<OutputStream, Hashtable<String, Term>> stream : outputStreams.entrySet()) {
                resultList.add(TuFactory.createTuAtom(stream.getKey().toString()));
            }
            Term result = createTuListStruct(resultList.toArray(new Term[1]));
            return unify(list, result);
        } else {
            for (Map.Entry<InputStream, Hashtable<String, Term>> currentElement : inputStreams.entrySet()) {
                for (Map.Entry<String, Term> currentElement2 : currentElement.getValue().entrySet()) {
                    if (currentElement2.getKey().equals(propertyName)) {
                        if (propertyName.equals("alias")) {
                            int arity = ((Term) currentElement2.getValue()).getPlArity();
                            if (arity == 0) {
                                if (propertyValue.equals((currentElement2.getValue()))) {
                                    resultList.add(TuFactory.createTuAtom(currentElement.getKey().toString()));
                                    break;
                                }
                            }
                            for (int i = 0; i < arity; i++) {
                                if (propertyValue.equals(((Term) currentElement2.getValue()).getPlainArg(i))) {
                                    resultList.add(TuFactory.createTuAtom(currentElement.getKey().toString()));
                                    break;
                                }
                            }
                        } else if (currentElement2.getValue().equals(propertyValue)) {
                            resultList.add(TuFactory.createTuAtom(currentElement.getKey().toString()));
                        }
                    }
                }
            }

            for (Map.Entry<OutputStream, Hashtable<String, Term>> currentElement : outputStreams.entrySet()) {
                for (Map.Entry<String, Term> currentElement2 : currentElement.getValue().entrySet()) {
                    if (currentElement2.getKey().equals(propertyName)) {
                        if (propertyName.equals("alias")) {
                            int arity = ((Term) currentElement2.getValue()).getPlArity();
                            if (arity == 0) {
                                if (propertyValue.equals((currentElement2.getValue()))) {
                                    resultList.add(TuFactory.createTuAtom(currentElement.getKey().toString()));
                                    break;
                                }
                            }
                            for (int i = 0; i < arity; i++) {
                                if (propertyValue.equals(((Term) currentElement2.getValue()).getPlainArg(i))) {
                                    resultList.add(TuFactory.createTuAtom(currentElement.getKey().toString()));
                                    break;
                                }
                            }
                        } else if (currentElement2.getValue().equals(propertyValue)) {
                            resultList.add(TuFactory.createTuAtom(currentElement.getKey().toString()));
                        }
                    }
                }
            }
        }
        Term result = createTuListStruct(resultList.toArray(new Term[1]));
        return unify(list, result);
    }

    //stream_property_2(Stream, Property): find_property restituisce la lista
    //degli stream che soddisfano quella proprieta' e member verifica l'appartenenza di S a quella lista
    @Override
    public String getTheory() {
        return "stream_property(S,P) :- find_property(L,P),member(S,L).\n";
    }

    public boolean at_end_of_stream_0() throws TuPrologError {
        initLibrary();
        Hashtable<String, Term> entry = inputStreams.get(inputStream);
        Term value = entry.get("end_of_stream");
        Term eof = (Term) value;
        if (eof.fname().equals("not")) {
            return false;
        } else {
            return true;
        }
    }

    public boolean at_end_of_stream_1(Term stream_or_alias) throws TuPrologError {
        initLibrary();
        InputStream stream = find_input_stream(stream_or_alias);
        Hashtable<String, Term> entry = inputStreams.get(stream);
        Term value = entry.get("end_of_stream");
        Term eof = (Term) value;
        if (eof.fname().equals("not")) {
            return false;
        } else {
            return true;
        }
    }

    //modificare la posizione dello stream se la proprieta' reposition e' true
    public boolean set_stream_position_2(Term stream_or_alias, Term position) throws TuPrologError {
        //soltanto per inputStream!
        initLibrary();
        InputStream in = find_input_stream(stream_or_alias);
        Term reposition = null;
        BufferedInputStream buffer = null;

        if (position.isVar()) {
            throw instantiation_error(engine.getEngineManager(), 2);
        } else {
            if (!(position.isNumber())) {
                throw domain_error(engine.getEngineManager(), 2, "stream_position", position);
            }
        }

        Hashtable<String, Term> entry = inputStreams.get(in);
        reposition = entry.get("reposition");

        Term value = (Term) reposition;
        if (value.fname().equals("false")) {
            throw TuPrologError
                    .permission_error(engine.getEngineManager(), "reposition", "stream", stream_or_alias, createTuAtom("Stream has property reposition(false)"));
        }

        if (in instanceof BufferedInputStream) {
            buffer = (BufferedInputStream) in;
        }

        if (buffer.markSupported()) {
            try {
                buffer.reset();

                TuNumber n = (TuNumber) position;
                long pos = n.longValue();
                if (pos < 0) {
                    throw domain_error(engine.getEngineManager(), 2, "+long", position);
                }

                int size;
                size = in.available();

                if (pos > size) {
                    throw TuPrologError
                            .system_error(TuFactory.createTuAtom("Invalid operation. Input position is greater than file size."));
                }
                if (pos == size) {
                    entry.put("end_of_file", createTuAtom("at"));
                }

                buffer.skip(pos);
                final TuLong tuLong = createTuLong(pos);
                int new_pos = tuLong.intValue();
                entry.put("position", createTuInt(new_pos));
                inputStreams.put(buffer, entry);

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                throw TuPrologError
                        .system_error(TuFactory.createTuAtom("An error has occurred in method 'set_stream_position'."));
            }
        }
        return true;
    }

    public boolean flush_output_0() throws TuPrologError {
        initLibrary();
        try {
            outputStream.flush();
        } catch (IOException e) {
            throw system_error(TuFactory.createTuAtom("An error has occurred in method 'flush_output_0'."));
        }
        return true;
    }

    public boolean flush_output_1(Term stream_or_alias) throws TuPrologError {
        initLibrary();
        OutputStream stream = find_output_stream(stream_or_alias);
        try {
            stream.flush();
        } catch (IOException e) {
            throw system_error(TuFactory.createTuAtom("An error has occurred in method 'flush_output_1'."));
        }
        return true;
    }

    public boolean get_char_2(Term stream_or_alias, Term arg) throws TuPrologError {
        initLibrary();
        InputStream stream = find_input_stream(stream_or_alias);
        Character c = null;
        int value = 0;

        if (!(arg.isVar())) {
            throw instantiation_error(engine.getEngineManager(), 1);
        }

        Hashtable<String, Term> element = inputStreams.get(stream);

        Term struct_name = (Term) element.get("file_name");
        String file_name = struct_name.toString();

        Term type = (Term) element.get("type");
        if (type.fname().equals("binary")) {
            throw permission_error(engine
                    .getEngineManager(), "input", "binary_stream", stream_or_alias, createTuAtom("The target stream is associated with a binary stream."));
        }

        //se lo stream e' stdin, leggo il carattere esattamente come fa get0 di IOLib
        //stdin lo scrivo come stringa, non posso usare inputStreamName, perche' in quel campo ci deve rimanere lo stream corrente, e se e' stato cambiato, non ho piu' sdtin
        if (file_name.equals("stdin")) {
            IOLib.get0_1(arg);
            return true;
        }

        //se e' un file invece devo effettuare tutti i controlli sullo stream.
        try {
            TuNumber position = (TuNumber) (element.get("position"));
            Term eof = (Term) element.get("end_of_stream");

            //se e' stata raggiunta la fine del file, controllo ed eseguo l'azione prestabilita nelle opzioni al momento dell'apertura del file.
            if ((eof.fname()).equals("past")) {
                Term actionTemp = element.get("eof_action");
                String action = ((Term) actionTemp).fname();

                if (action.equals("error"))
                    throw TuPrologError
                            .permission_error(engine.getEngineManager(), "input", "past_end_of_stream", createTuAtom("reader"), createTuAtom("End of file is reached."));
                else if (action.equals("eof_code"))
                    return unify(arg, createTuAtom("-1"));
                else if (action.equals("reset")) {
                    element.put("end_of_stream", createTuAtom("not"));
                    element.put("position", createTuInt(0));
                    stream.reset();
                }
            }

            //effettuo la lettura, anche se dovevo fare reset dello stream
            value = stream.read();

            if (!Character.isDefined(value)) {
                if (value == -1) {
                    element.put("end_of_stream", createTuAtom("past"));
                } else {
                    throw representation_error(engine.getEngineManager(), 2, "character");
                }
            }
            TuInt i = (TuInt) position;
            int i2 = i.intValue();
            i2++;
            element.put("position", createTuInt(i2));

            if (value != -1) {
                //vado a controllare il prossimo carattere
                //se e' fine file, end_of_stream diventa "at"
                TuVar nextChar = createTuVar();
                peek_code_2(stream_or_alias, nextChar);
                Term nextCharTerm = nextChar.dref();
                TuNumber nextCharValue = (TuNumber) nextCharTerm;
                if (nextCharValue.intValue() == -1) {
                    element.put("end_of_stream", createTuAtom("at"));
                }
            }

            inputStreams.put(stream, element);

            if (value == -1) {
                return unify(arg, createTerm(value + ""));
            }
            c = new Character((char) value);
            return unify(arg, createTuAtom(c.toString()));
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw system_error(TuFactory.createTuAtom("An I/O error has occurred"));
        }
    }

    public boolean get_code_1(Term char_code) throws TuPrologError {
        initLibrary();
        Term s_or_a = createTuAtom(inputStream.toString());
        return get_code_2(s_or_a, char_code);
    }

    public boolean get_code_2(Term stream_or_alias, Term char_code) throws TuPrologError {
        initLibrary();
        InputStream stream = find_input_stream(stream_or_alias);

        int value = 0;

        if (!(char_code.isVar())) {
            throw instantiation_error(engine.getEngineManager(), 1);
        }

        Hashtable<String, Term> element = inputStreams.get(stream);
        Term type = (Term) element.get("type");
        if (type.fname().equals("binary")) {
            throw permission_error(engine
                    .getEngineManager(), "input", "binary_stream", stream_or_alias, createTuAtom("The target stream is associated with a binary stream."));
        }

        //se file_name e' stdin leggo il codice del carattere normalmente
        //senza preoccuparmi di controllare tutte le opzioni dello stream. 
        Term struct_name = (Term) element.get("file_name");
        String file_name = struct_name.toString();
        if (file_name.equals("stdin")) {
            try {
                value = inputStream.read();
            } catch (IOException e) {
                throw permission_error(engine.getEngineManager(), "input", "stream", createTuAtom(inputStreamName), createTuAtom(e.getMessage()));
            }

            if (value == -1) {
                return unify(char_code, createTuInt(-1));
            } else {
                return unify(char_code, createTuInt(value));
            }
        }

        //se invece lo stream e' un normale file, devo controllare tutte le opzioni decise in apertura.
        try {
            TuNumber position = (TuNumber) (element.get("position"));
            Term eof = (Term) element.get("end_of_stream");
            if (eof.equals("past")) {
                Term actionTemp = element.get("eof_action");
                String action = ((Term) actionTemp).fname();
                if (action.equals("error")) {
                    throw TuPrologError
                            .permission_error(engine.getEngineManager(), "input", "past_end_of_stream", createTuAtom("reader"), createTuAtom("End of file is reached."));
                } else if (action.equals("eof_code")) {
                    return unify(char_code, createTuAtom("-1"));
                } else if (action.equals("reset")) {
                    element.put("end_of_stream", createTuAtom("not"));
                    element.put("position", createTuInt(0));
                    stream.reset();
                }
            }

            value = stream.read();

            if (!Character.isDefined(value)) {
                if (value == -1) {
                    element.put("end_of_stream", createTuAtom("past"));
                } else {
                    throw representation_error(engine.getEngineManager(), 2, "character");
                }
            }
            TuInt i = (TuInt) position;
            int i2 = i.intValue();
            i2++;
            element.put("position", createTuInt(i2));

            if (value != -1) {
                TuVar nextChar = createTuVar();
                peek_code_2(stream_or_alias, nextChar);
                Term nextCharTerm = nextChar.dref();
                TuNumber nextCharValue = (TuNumber) nextCharTerm;
                if (nextCharValue.intValue() == -1) {
                    element.put("end_of_stream", createTuAtom("at"));
                }
            }

            inputStreams.put(stream, element);
            return unify(char_code, createTuInt(value));
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw system_error(TuFactory.createTuAtom("An I/O error has occurred."));
        }
    }

    public boolean peek_char_1(Term in_char) throws TuPrologError {
        initLibrary();
        Term s_or_a = createTuAtom(inputStream.toString());
        if (inputStreamName.equals("stdin")) {
            inputStream.mark(5);
            boolean var = get_char_2(s_or_a, in_char);
            try {
                inputStream.reset();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                system_error(TuFactory.createTuAtom("An error has occurred in peek_char_1."));
            }
            return var;
        } else {
            return peek_char_2(s_or_a, in_char);
        }
    }

    public boolean peek_char_2(Term stream_or_alias, Term in_char) throws TuPrologError {
        //come la get_char soltanto non cambio la posizione di lettura
        initLibrary();
        InputStream stream = find_input_stream(stream_or_alias);
        Hashtable<String, Term> element = inputStreams.get(stream);
        String file_name = ((Term) element.get("file_name")).fname();

        if (file_name.equals("stdin")) {
            return get_char_2(stream_or_alias, in_char);
        }

        FileInputStream stream2 = null;
        try {
            stream2 = new FileInputStream(file_name);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            system_error(TuFactory.createTuAtom("File not found."));
        }
        Character c = null;
        int value = 0;

        if (!(in_char.isVar())) {
            throw instantiation_error(engine.getEngineManager(), 1);
        }
        Term type = (Term) element.get("type");
        if (type.fname().equals("binary")) {
            throw permission_error(engine
                    .getEngineManager(), "input", "binary_stream", stream_or_alias, createTuAtom("Target stream is associated with a binary stream."));
        }

        try {
            TuNumber position = (TuNumber) (element.get("position"));
            Term eof = (Term) element.get("end_of_stream");
            if (eof.equals("past")) {
                Term actionTemp = element.get("eof_action");
                String action = ((Term) actionTemp).fname();
                if (action.equals("error")) {
                    throw TuPrologError
                            .permission_error(engine.getEngineManager(), "input", "past_end_of_stream", createTuAtom("reader"), createTuAtom("End of file has been reached."));
                } else if (action.equals("eof_code")) {
                    return unify(in_char, createTuAtom("-1"));
                } else if (action.equals("reset")) {
                    element.put("end_of_stream", createTuAtom("not"));
                    element.put("position", createTuInt(0));
                    stream.reset();
                }
            } else {
                TuInt i = (TuInt) position;
                long nBytes = i.longValue();
                stream2.skip(nBytes);
                value = stream2.read();

                stream2.close();
            }
            if (!Character.isDefined(value) && value != -1) { //non devo nemmeno settare a eof la propriet? perch? la posizone
                //dello stream deve rimanere inalterata.
                throw representation_error(engine.getEngineManager(), 2, "character");
            }
            inputStreams.put(stream, element);

            if (value == -1) {
                return unify(in_char, createTerm(value + ""));
            }

            c = new Character((char) value);
            return unify(in_char, createTerm(c.toString()));
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw system_error(TuFactory.createTuAtom("An I/O error has occurred."));
        }
    }

    public boolean peek_code_1(Term char_code) throws TuPrologError {
        initLibrary();
        Term stream = createTuAtom(inputStream.toString());
        if (inputStreamName.equals("stdin")) {
            inputStream.mark(5);
            boolean var = get_code_2(stream, char_code);
            try {
                inputStream.reset();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                system_error(TuFactory.createTuAtom("An error has occurred in peek_code_1."));
            }
            return var;
        } else {
            return peek_char_2(stream, char_code);
        }
    }

    public boolean peek_code_2(Term stream_or_alias, Term char_code) throws TuPrologError {
        initLibrary();
        //come la get_char soltanto non cambio la posizione di lettura
        InputStream stream = find_input_stream(stream_or_alias);
        Hashtable<String, Term> element = inputStreams.get(stream);
        String file_name = ((Term) element.get("file_name")).fname();

        FileInputStream stream2 = null;
        try {
            stream2 = new FileInputStream(file_name);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            system_error(TuFactory.createTuAtom("File not found."));
        }
        int value = 0;

        if (!(char_code.isVar())) {
            throw instantiation_error(engine.getEngineManager(), 1);
        }
        Term type = (Term) element.get("type");
        if (type.fname().equals("binary")) {
            throw permission_error(engine
                    .getEngineManager(), "input", "binary_stream", stream_or_alias, createTuAtom("Target stream is associated with a binary stream."));
        }

        try {
            TuNumber position = (TuNumber) (element.get("position"));
            Term eof = (Term) element.get("end_of_stream");
            if (eof.equals("past")) {
                Term actionTemp = element.get("eof_action");
                String action = ((Term) actionTemp).fname();
                if (action.equals("error")) {
                    throw TuPrologError
                            .permission_error(engine.getEngineManager(), "input", "past_end_of_stream", createTuAtom("reader"), createTuAtom("End of file is reached."));
                } else if (action.equals("eof_code")) {
                    return unify(char_code, createTuAtom("-1"));
                } else if (action.equals("reset")) {
                    element.put("end_of_stream", createTuAtom("not"));
                    element.put("position", createTuInt(0));
                    stream.reset();
                }
            } else {
                TuInt i = (TuInt) position;
                long nBytes = i.longValue();
                stream2.skip(nBytes);
                value = stream2.read();
                stream2.close();
            }
            if (!Character.isDefined(value) && value != -1) { //non devo nemmeno settare a eof la proprieta' perche' la posizone
                                                              //dello stream deve rimanere inalterata.
                throw representation_error(engine.getEngineManager(), 2, "character");
            }
            inputStreams.put(stream, element);
            return unify(char_code, createTuInt(value));
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw system_error(TuFactory.createTuAtom("An I/O error has occurred."));
        }
    }

    public boolean put_char_2(Term stream_or_alias, Term in_char) throws TuPrologError {
        initLibrary();
        OutputStream stream = find_output_stream(stream_or_alias);
        String stream_name = get_output_name(stream);

        Hashtable<String, Term> element = outputStreams.get(stream);
        Term type = (Term) element.get("type");
        if (type.fname().equals("binary")) {
            throw permission_error(engine
                    .getEngineManager(), "input", "binary_stream", stream_or_alias, createTuAtom("Target stream is associated with a binary stream."));
        }

        Term arg0 = (Term) in_char.dref();

        if (arg0.isVar())
            throw instantiation_error(engine.getEngineManager(), 2);
        else if (!arg0.isAtomSymbol()) {
            throw type_error(engine.getEngineManager(), 2, "character", arg0);
        } else {
            String ch = arg0.fname();
            if (!(Character.isDefined(ch.charAt(0)))) {
                throw representation_error(engine.getEngineManager(), 2, "character");
            }
            if (ch.length() > 1) {
                throw type_error(engine.getEngineManager(), 2, "character", createTuAtom(ch));
            } else {
                if (stream_name.equals("stdout")) {
                    getEngine().stdOutput(ch);
                } else {
                    try {
                        stream.write((byte) ch.charAt(0));
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                        throw system_error(TuFactory.createTuAtom("An I/O error has occurred."));
                    }
                }
                return true;
            }
        }
    }

    public boolean put_code_1(Term char_code) throws TuPrologError {
        initLibrary();
        Term stream = createTuAtom(outputStream.toString());
        return put_code_2(stream, char_code);
    }

    public boolean put_code_2(Term stream_or_alias, Term char_code) throws TuPrologError {
        initLibrary();
        OutputStream stream = find_output_stream(stream_or_alias);
        String stream_name = get_output_name(stream);

        Hashtable<String, Term> element = outputStreams.get(stream);
        Term type = (Term) element.get("type");
        if (type.fname().equals("binary")) {
            throw permission_error(engine
                    .getEngineManager(), "input", "binary_stream", stream_or_alias, createTuAtom("Target stream is associated with a binary stream."));
        }

        TuNumber arg0 = (TuNumber) char_code.dref();

        if (arg0.isVar()) {
            throw instantiation_error(engine.getEngineManager(), 2);
        } else if (!arg0.isNumber()) {
            throw type_error(engine.getEngineManager(), 2, "character", arg0);
        } else {
            if (Character.isDefined(arg0.intValue())) {
                throw representation_error(engine.getEngineManager(), 2, "character_code");
            }
            if (stream_name.equals("stdout")) {
                getEngine().stdOutput("" + arg0.intValue());
            } else {
                try {
                    stream.write(arg0.intValue());
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    throw system_error(TuFactory.createTuAtom("An I/O error has occurred."));
                }
            }
        }
        return true;
    }

    public boolean nl_1(Term stream_or_alias) throws TuPrologError {
        initLibrary();
        OutputStream stream = find_output_stream(stream_or_alias);
        String stream_name = get_output_name(stream);
        if (stream_name.equals("stdout")) {
            getEngine().stdOutput("\n");
        } else {
            try {
                stream.write('\n');
            } catch (IOException e) {
                throw permission_error(engine.getEngineManager(), "output", "stream", createTuAtom(outputStreamName), createTuAtom(e.getMessage()));
            }
        }
        return true;
    }

    public boolean get_byte_1(Term in_byte) throws TuPrologError {
        //non faccio la stessa struttura della get_char perch? stdin e stdout sono type=text e non posso fare la get_byte su di loro
        //lo stesso vale per tutti gli altri predicati
        initLibrary();
        Term stream_or_alias = createTuAtom(inputStream.toString());
        return get_byte_2(stream_or_alias, in_byte);
    }

    public boolean get_byte_2(Term stream_or_alias, Term in_byte) throws TuPrologError {
        initLibrary();
        InputStream stream = find_input_stream(stream_or_alias);
        Byte b = null;
        Hashtable<String, Term> element = inputStreams.get(stream);
        Term type = (Term) element.get("type");
        if (type.fname().equals("text")) {
            throw TuPrologError
                    .permission_error(engine.getEngineManager(), "input", "text_stream", stream_or_alias, createTuAtom("Target stream is associated with a text stream."));
        }

        if (!(in_byte.isVar()))
            throw instantiation_error(engine.getEngineManager(), 1);

        try {
            DataInputStream reader = new DataInputStream(stream);
            TuNumber position = (TuNumber) (element.get("position"));
            TuInt i = (TuInt) position;
            int i2 = i.intValue();
            reader.skipBytes(i2 - 1);
            Term eof = (Term) element.get("end_of_stream");
            if (eof.equals("past")) {
                Term actionTemp = element.get("eof_action");
                String action = ((Term) actionTemp).fname();
                if (action.equals("error")) {
                    throw TuPrologError
                            .permission_error(engine.getEngineManager(), "input", "past_end_of_stream", createTuAtom("reader"), createTuAtom("End of file is reached."));
                } else if (action.equals("eof_code")) {
                    return unify(in_byte, createTuAtom("-1"));
                } else if (action.equals("reset")) {
                    element.put("end_of_stream", createTuAtom("not"));
                    element.put("position", createTuInt(0));
                    reader.reset();
                }

            }

            b = reader.readByte();

            i2++; //incremento la posizione dello stream
            element.put("position", createTuInt(i2));

            //if(b != -1){
            TuVar nextByte = createTuVar();
            peek_byte_2(stream_or_alias, nextByte);
            Term nextByteTerm = nextByte.dref();
            TuNumber nextByteValue = (TuNumber) nextByteTerm;
            if (nextByteValue.intValue() == -1) {
                element.put("end_of_stream", createTuAtom("at"));
            }
            //}

            inputStreams.put(stream, element);
            return unify(in_byte, createTerm(b.toString()));
        } catch (IOException ioe) {
            element.put("end_of_stream", createTuAtom("past"));
            return unify(in_byte, createTerm("-1"));
        }
    }

    public boolean peek_byte_1(Term in_byte) throws TuPrologError {
        initLibrary();
        Term stream_or_alias = createTuAtom(inputStream.toString());
        return peek_char_2(stream_or_alias, in_byte);
    }

    public boolean peek_byte_2(Term stream_or_alias, Term in_byte) throws TuPrologError {
        initLibrary();
        InputStream stream = find_input_stream(stream_or_alias);
        Byte b = null;
        Hashtable<String, Term> element = inputStreams.get(stream);
        Term type = (Term) element.get("type");
        if (type.fname().equals("text")) {
            throw TuPrologError
                    .permission_error(engine.getEngineManager(), "input", "text_stream", stream_or_alias, createTuAtom("Target stream is associated with a text stream."));
        }

        if (!(in_byte.isVar()))
            throw instantiation_error(engine.getEngineManager(), 1);

        try {
            DataInputStream reader = new DataInputStream(stream);
            TuNumber position = (TuNumber) (element.get("position"));
            TuInt i = (TuInt) position;
            int i2 = i.intValue();
            reader.skipBytes(i2 - 2);
            Term eof = (Term) element.get("end_of_stream");
            if (eof.equals("past")) {
                Term actionTemp = element.get("eof_action");
                String action = ((Term) actionTemp).fname();
                if (action.equals("error")) {
                    throw TuPrologError
                            .permission_error(engine.getEngineManager(), "input", "past_end_of_stream", createTuAtom("reader"), createTuAtom("End of file is reached."));
                } else if (action.equals("eof_code")) {
                    return unify(in_byte, createTuAtom("-1"));
                } else if (action.equals("reset")) {
                    element.put("end_of_stream", createTuAtom("not"));
                    element.put("position", createTuInt(0));
                    reader.reset();
                }

            } else {
                b = reader.readByte();
            }

            inputStreams.put(stream, element);
            return unify(in_byte, createTerm(b.toString()));
        } catch (IOException e) {
            element.put("end_of_stream", createTuAtom("past"));
            return unify(in_byte, createTerm("-1"));
        }
    }

    public boolean put_byte_1(Term out_byte) throws TuPrologError {
        //        //richiamo il metodo di IOLibrary che lavora sullo stream corrente
        //        //la posso utilizzare cosi' com'e' in quanto presenta gia' tutti i controlli sul parametro arg richiesti
        //        initLibrary();
        //        out_byte = out_byte.getTerm();
        //        return IOLib.put_1(out_byte);

        initLibrary();
        Term stream_or_alias = createTuAtom(outputStream.toString());
        return put_byte_2(stream_or_alias, out_byte);

    }

    public boolean put_byte_2(Term stream_or_alias, Term out_byte) throws TuPrologError {
        initLibrary();
        OutputStream stream = find_output_stream(stream_or_alias);
        out_byte = out_byte.dref();
        TuNumber b = (TuNumber) out_byte.dref();

        Hashtable<String, Term> element = outputStreams.get(stream);
        Term type = (Term) element.get("type");
        if (type.fname().equals("text")) {
            throw TuPrologError
                    .permission_error(engine.getEngineManager(), "output", "text_stream", stream_or_alias, createTuAtom("Target stream is associated with a text stream."));
        }

        if (out_byte.isVar())
            throw instantiation_error(engine.getEngineManager(), 1);

        if (stream.equals("stdout")) {
            //lo standard output non e' uno stream binario, 
            //se si tenta di scrivere su std output questo mi restituisce soltanto la stringa.
            getEngine().stdOutput(out_byte.toString()); //da riguardare!!
        } else {
            try {
                DataOutputStream writer = new DataOutputStream(stream);
                TuNumber position = (TuNumber) (element.get("position"));
                TuInt i = (TuInt) position;
                int i2 = i.intValue();

                writer.writeByte(b.intValue());

                i2++;
                element.put("position", createTuInt(i2));
                outputStreams.put(stream, element);
            } catch (IOException e) {
                throw permission_error(engine.getEngineManager(), "output", "stream", createTuAtom(outputStreamName), createTuAtom(e.getMessage()));
            }
        }
        return true;
    }

    public boolean read_term_2(Term in_term, Term options) throws TuPrologError {
        initLibrary();
        Term stream_or_alias = createTuAtom(inputStream.toString());
        return read_term_3(stream_or_alias, in_term, options);
    }

    public boolean read_term_3(Term stream_or_alias, Term in_term, Term options) throws TuPrologError {
        initLibrary();
        InputStream stream = find_input_stream(stream_or_alias);

        if (options.isVar()) {
            throw instantiation_error(engine.getEngineManager(), 3);
        }

        Hashtable<String, Term> element = inputStreams.get(stream);
        Term type = (Term) element.get("type");
        Term eof = (Term) element.get("end_of_stream");
        Term action = (Term) element.get("eof_action");
        TuNumber position = (TuNumber) element.get("position");
        if (type.fname().equals("binary")) {
            throw permission_error(engine
                    .getEngineManager(), "input", "binary_stream", stream_or_alias, createTuAtom("Target stream is associated with a binary stream."));
        }
        if ((eof.fname()).equals("past") && (action.fname()).equals("error")) {
            throw permission_error(engine
                    .getEngineManager(), "past_end_of_stream", "stream", stream_or_alias, createTuAtom("Target stream has position at past_end_of_stream"));
        }

        Term variables = null;
        Term variable_names = null;
        Term singletons = null;

        boolean variables_bool = false;
        boolean variable_names_bool = false;
        boolean singletons_bool = false;

        Term readOptions = (Term) options;
        if (readOptions.isPlList()) {
            if (!readOptions.isEmptyList()) {
                Iterator<? extends Term> i = readOptions.listIteratorProlog();
                while (i.hasNext()) {
                    Term option = null;
                    Term obj = i.next();
                    if (obj.isVar()) {
                        throw instantiation_error(engine.getEngineManager(), 3);
                    }
                    option = (Term) obj;
                    if (option.fname().equals("variables")) {
                        variables_bool = true;
                    } else if (option.fname().equals("variable_name")) {
                        variable_names_bool = true;
                    } else if (option.fname().equals("singletons")) {
                        singletons_bool = true;
                    } else {
                        domain_error(engine.getEngineManager(), 3, "read_option", option);
                    }
                }
            }
        } else {
            throw type_error(engine.getEngineManager(), 3, "list", options);
        }

        try {
            int ch = 0;

            boolean open_apices = false;
            boolean open_apices2 = false;

            in_term = in_term.dref();
            String st = "";
            do {
                ch = stream.read();

                if (ch == -1) {
                    break;
                }
                boolean can_add = true;

                if (ch == '\'') {
                    if (!open_apices) {
                        open_apices = true;
                    } else {
                        open_apices = false;
                    }
                } else if (ch == '\"') {
                    if (!open_apices2) {
                        open_apices2 = true;
                    } else {
                        open_apices2 = false;
                    }
                } else {
                    if (ch == '.') {
                        if (!open_apices && !open_apices2) {
                            break;
                        }
                    }
                }
                if (can_add) {
                    st += new Character(((char) ch)).toString();
                }
            } while (true);

            TuInt p = (TuInt) position;
            int p2 = p.intValue();
            p2 += (st.getBytes()).length;

            if (ch == -1) {
                st = "-1";
                element.put("end_of_stream", createTuAtom("past"));
                element.put("position", createTuInt(p2));
                inputStreams.put(stream, element);
                return unify(in_term, createTerm(st));
            }

            if (variables_bool == false && variable_names_bool == false && singletons_bool == false) {
                return unify(in_term, getEngine().toTerm(st));
            }
            TuVar input_term = createTuVar();
            unify(input_term, createTerm(st));

            //opzione variables + variables_name
            List<Term> variables_list = new ArrayList<Term>();
            analize_term(variables_list, input_term);

            Hashtable<Term, String> associations_table = new Hashtable<Term, String>(variables_list.size());

            //la hashtable sottostante la costruisco per avere le associazioni 
            //con le variabili '_' Queste infatti non andrebbero inserite all'interno della
            //read_option variable_name, ma vanno sostituite comunque da variabili nel termine letto.
            Hashtable<Term, String> association_for_replace = new Hashtable<Term, String>(variables_list.size());

            LinkedHashSet<Term> set = new LinkedHashSet<Term>(variables_list);
            List<TuVar> vars = new ArrayList<TuVar>();

            if (variables_bool == true) {
                int num = 0;
                for (Term t : set) {
                    num++;
                    if (variable_names_bool == true) {
                        association_for_replace.put(t, "X" + num);
                        if (!((t.toString()).startsWith("_"))) {
                            associations_table.put(t, "X" + num);
                        }
                    }
                    vars.add(TuFactory.createTuVarNamed("X" + num));
                }
            }

            //opzione singletons
            List<Term> singl = new ArrayList<Term>();
            int flag = 0;
            if (singletons_bool == true) {
                List<Term> temporanyList = new ArrayList<Term>(variables_list);
                for (Term t : variables_list) {
                    temporanyList.remove(t);
                    flag = 0;
                    for (Term temp : temporanyList) {
                        if (temp.equals(t)) {
                            flag = 1;
                        }
                    }
                    if (flag == 0) {
                        if (!((t.toString()).startsWith("_"))) {
                            singl.add(t);
                        }
                    }
                    temporanyList.add(t);
                }
            }

            //unisco le liste con i relativi termini
            Iterator<? extends Term> i = readOptions.listIteratorProlog();
            Term option = null;
            while (i.hasNext()) {
                Object obj = i.next();
                option = (Term) obj;
                if (option.fname().equals("variables")) {
                    variables = createStructEmpty();
                    variables = (Term) createTerm(vars.toString());
                    unify(option.getPlainArg(0), variables);
                } else if (option.fname().equals("variable_name")) {
                    variable_names = createStructEmpty();
                    variable_names = (Term) createTerm(associations_table.toString());
                    unify(option.getPlainArg(0), variable_names);
                } else if (option.fname().equals("singletons")) {
                    singletons = createStructEmpty();
                    singletons = (Term) createTerm(singl.toString());
                    unify(option.getPlainArg(0), singletons);
                }
            }

            String string_term = input_term.toString();

            for (Map.Entry<Term, String> entry : association_for_replace.entrySet()) {
                String regex = entry.getKey().toString();
                String replacement = entry.getValue();
                string_term = string_term.replaceAll(regex, replacement);
            }

            //vado a modificare la posizione di lettura
            element.put("position", createTuInt(p2));
            inputStreams.put(stream, element);
            return unify(in_term, getEngine().toTerm(string_term));
        } catch (Exception ex) {
            return false;
        }
    }

    private void analize_term(List<Term> variables, Term t) {
        if (!t.isCompound()) {
            variables.add(t);
        } else {
            Term term_struct = (Term) t.dref();
            for (int i = 0; i < term_struct.getPlArity(); i++) {
                analize_term(variables, term_struct.getPlainArg(i));
            }
        }
    }

    public boolean read_2(Term stream_or_alias, Term in_term) throws TuPrologError {
        initLibrary();
        Term options = S(".", createTuEmpty());
        return read_term_3(stream_or_alias, in_term, options);
    }

    public boolean write_term_2(Term out_term, Term options) throws TuPrologError {
        initLibrary();
        Term stream_or_alias = createTuAtom(outputStream.toString());
        return write_term_3(stream_or_alias, out_term, options);
    }

    public boolean write_term_3(Term stream_or_alias, Term out_term, Term optionsTerm) throws TuPrologError {
        initLibrary();
        out_term = out_term.dref();

        OutputStream output = find_output_stream(stream_or_alias);
        String output_name = get_output_name(output);
        Term writeOptionsList = (Term) optionsTerm.dref();

        boolean quoted = false;
        boolean ignore_ops = false;
        boolean numbervars = false;
        Term writeOption = null;

        Hashtable<String, Term> element = outputStreams.get(output);
        Term type = (Term) element.get("type");
        if (type.fname().equals("binary")) {
            throw permission_error(engine
                    .getEngineManager(), "output", "binary_stream", stream_or_alias, createTuAtom("Target stream is associated with a binary stream."));
        }

        if (writeOptionsList.isPlList()) {
            if (!writeOptionsList.isEmptyList()) {
                Iterator<? extends Term> i = writeOptionsList.listIteratorProlog();
                while (i.hasNext()) {
                    //siccome queste opzioni sono true o false analizzo direttamente il loro valore
                    //e restituisco il loro valore all'interno dell'opzione corrispondente
                    Term obj = i.next();
                    if (obj.isVar()) {
                        throw instantiation_error(engine.getEngineManager(), 3);
                    }
                    writeOption = (Term) obj;
                    if (writeOption.fname().equals("quoted")) {
                        quoted = ((Term) writeOption.getPlainArg(0)).fname().equals("true") ? true : false;
                    } else if (writeOption.fname().equals("ignore_ops")) {
                        ignore_ops = ((Term) writeOption.getPlainArg(0)).fname().equals("true") ? true : false;
                    } else if (writeOption.fname().equals("numbervars")) {
                        numbervars = ((Term) writeOption.getPlainArg(0)).fname().equals("true") ? true : false;
                    } else {
                        throw domain_error(engine.getEngineManager(), 3, "write_options", writeOptionsList
                                .dref());
                    }
                }
            }
        } else {
            type_error(engine.getEngineManager(), 3, "list", writeOptionsList);
        }
        try {
            if (!out_term.isCompound() && !(out_term.isVar())) {

                if (output_name.equals("stdout")) {
                    if (quoted == true) { //per scrivere sull'output devo richiamare l'output dell'Engine nel caso di stdio,
                                          //altrimenti utilizzando write() il risultato lo stampa sulla console Java.
                                          //Nel caso in cui l'output e' un file write e' corretto.
                        getEngine().stdOutput((alice.util.Tools.removeApices(out_term.toString())));
                    } else {
                        getEngine().stdOutput((out_term.toString()));
                    }
                } else {
                    if (quoted == true) {
                        output.write((alice.util.Tools.removeApices(out_term.toString())).getBytes());
                    } else {
                        output.write((out_term.toString()).getBytes());
                    }
                }

                return true;
            }

            if (out_term.isVar()) {

                if (output_name.equals("stdout")) {
                    if (quoted == true) {
                        getEngine().stdOutput((alice.util.Tools.removeApices(out_term.toString()) + " "));
                    } else {
                        getEngine().stdOutput((out_term.toString() + " "));
                    }
                } else {
                    if (quoted == true) {
                        output.write((alice.util.Tools.removeApices(out_term.toString()) + " ").getBytes());
                    } else {
                        output.write((out_term.toString() + " ").getBytes());
                    }
                }

                return true;
            }

            Term term = (Term) out_term;
            String result = "";
            Hashtable<String, Boolean> options = new Hashtable<String, Boolean>(3);
            options.put("numbervars", numbervars);
            options.put("ignore_ops", ignore_ops);
            options.put("quoted", quoted);

            result = create_string(options, term);

            if (output_name.equals("stdout")) {
                getEngine().stdOutput(result);
            } else {
                output.write((result + " ").getBytes());
            }

        } catch (IOException ioe) {
            system_error(TuFactory.createTuAtom("Write error has occurred in write_term/3."));
        }
        return true;
    }

    private String create_string(Hashtable<String, Boolean> options, Term term) {

        boolean numbervars = options.get("numbervars");
        boolean quoted = options.get("quoted");
        boolean ignore_ops = options.get("ignore_ops");

        String result = "";
        String list = "";
        if (term.isPlList()) {
            list = print_list(term, options);
            if (ignore_ops == false)
                return "[" + list + "]";
            else
                return list;
        }

        List<TuOperator> operatorList = engine.getCurrentOperatorList();
        String operator = "";
        int flagOp = 0;
        for (TuOperator op : operatorList) {
            if (op.name.equals(term.fname())) {
                operator = op.name;
                flagOp = 1;
                break;
            }
        }

        if (flagOp == 0) {
            result += term.fname() + "(";
        }

        int arity = term.getPlArity();
        for (int i = 0; i < arity; i++) {
            if (i > 0 && flagOp == 0)
                result += ",";
            Term arg = term.getPlainArg(i);
            if (arg.isNumber()) {
                if (term.fname().contains("$VAR")) {
                    //sono nel tipo $VAR
                    if (numbervars == true) {
                        TuInt argNumber = (TuInt) term.getPlainArg(i);
                        int res = argNumber.intValue() % 26;
                        int div = argNumber.intValue() / 26;
                        Character ch = 'A';
                        int num = (ch + res);
                        result = new String(Character.toChars(num));
                        if (div != 0) {
                            result += div;
                        }
                    } else {
                        if (quoted == true) {
                            return term.toString();
                        } else {
                            result += alice.util.Tools.removeApices(arg.toString());
                        }
                    }
                    continue;
                } else {
                    //e' un numero da solo o un operando
                    if (ignore_ops == false) {
                        result += arg.toString();
                        if (i % 2 == 0 && operator != "") {
                            result += " " + operator + " ";
                        }
                        continue;
                    } else {
                        result = term.toString();
                        return result;
                    }
                }
            } else if (arg.isVar()) {
                // stampo il toString della variabile
                if (ignore_ops == false) {
                    result += arg.toString();
                    if (i % 2 == 0 && operator != "") {
                        result += " " + operator + " ";
                    }
                    continue;
                } else {
                    result += arg.toString();
                }
                continue;
            } else if (arg.isCompound()) {
                if (ignore_ops == false) {
                    result += create_string(options, (Term) arg);
                    if (i % 2 == 0 && operator != "") {
                        result += " " + operator + " ";
                    }
                    continue;
                } else {
                    result += create_string(options, (Term) arg);
                }

            } else {
                if (quoted == true) {
                    if (ignore_ops == false) {
                        result += arg.toString();
                        if (i % 2 == 0 && operator != "") {
                            result += " " + operator + " ";
                        }
                        continue;
                    } else {
                        result += arg.toString();
                    }
                }

                else {
                    if (ignore_ops == false) {
                        result += alice.util.Tools.removeApices(arg.toString());
                        if (i % 2 == 0 && operator != "") {
                            result += " " + operator + " ";
                        }
                        continue;
                    } else {
                        result += alice.util.Tools.removeApices(arg.toString());
                    }
                }
            }
        }

        if (flagOp == 0 && result.contains("(")) {
            result += ")";
        }
        return result;
    }

    private String print_list(Term term, Hashtable<String, Boolean> options) {

        //boolean numbervars = options.get("numbervars");
        //boolean quoted = options.get("quoted");
        boolean ignore_ops = options.get("ignore_ops");

        String result = "";

        if (ignore_ops == true) {
            result = "'" + term.fname() + "'" + " (";
            for (int i = 0; i < term.getPlArity(); i++) {
                if (i > 0) {
                    result += ",";
                }
                if (term.getPlainArg(i).isPlList() && !(term.getPlainArg(i).isEmptyList())) {
                    result += print_list((Term) term.getPlainArg(i), options);
                } else {
                    result += term.getPlainArg(i);
                }
            }
            return result + ")";
        } else {
            for (int i = 0; i < term.getPlArity(); i++) {
                if (i > 0 && !(term.getPlainArg(i).isEmptyList())) {
                    result += ",";
                }
                if ((term.getPlainArg(i)).isCompound() && !(term.getPlainArg(i).isPlList())) {
                    result += create_string(options, (Term) term.getPlainArg(i));
                } else {
                    //costruito cosi' per un problema di rappresentazione delle []
                    if ((term.getPlainArg(i).isPlList()) && !(term.getPlainArg(i).isEmptyList()))
                        result += print_list((Term) term.getPlainArg(i), options);
                    else {
                        if (!(term.getPlainArg(i).isEmptyList()))
                            result += term.getPlainArg(i).toString();
                    }

                }
            }
            return result;
        }
    }

    public boolean write_2(Term stream_or_alias, Term out_term) throws TuPrologError {
        initLibrary();
        Term options = createTuCons( S("quoted", createTuAtom("false")), TuFactory
                .createTuCons( S("ignore_ops", createTuAtom("false")), createTuCons( S("numbervars", createTuAtom("true")), createTuEmpty())));
        return write_term_3(stream_or_alias, out_term, options);
    }

    public boolean write_1(Term out_term) throws TuPrologError {
        if (write_flag == 0) {
            return write_iso_1(out_term);
        } else {
            return IOLib.write_base_1(out_term);
        }
    }

    public boolean write_iso_1(Term out_term) throws TuPrologError {
        initLibrary();
        Term stream_or_alias = createTuAtom(outputStream.toString());
        Term options = createTuCons( S("quoted", createTuAtom("false")), TuFactory
                .createTuCons( S("ignore_ops", createTuAtom("false")), createTuCons( S("numbervars", createTuAtom("true")), createTuEmpty())));
        return write_term_3(stream_or_alias, out_term, options);
    }

    public boolean writeq_1(Term out_term) throws TuPrologError {
        initLibrary();
        Term stream_or_alias = createTuAtom(outputStream.toString());
        Term options = createTuCons( S("quoted", createTuAtom("true")), TuFactory
                .createTuCons( S("ignore_ops", createTuAtom("false")), createTuCons( S("numbervars", createTuAtom("true")), createTuEmpty())));
        return write_term_3(stream_or_alias, out_term, options);
    }

    public boolean writeq_2(Term stream_or_alias, Term out_term) throws TuPrologError {
        initLibrary();
        Term options = createTuCons( S("quoted", createTuAtom("true")), TuFactory
                .createTuCons( S("ignore_ops", createTuAtom("false")), createTuCons( S("numbervars", createTuAtom("true")), createTuEmpty())));
        return write_term_3(stream_or_alias, out_term, options);
    }

    public boolean write_canonical_1(Term out_term) throws TuPrologError {
        initLibrary();
        Term stream_or_alias = createTuAtom(outputStream.toString());
        Term options = createTuCons( S("quoted", createTuAtom("true")), TuFactory
                .createTuCons( S("ignore_ops", createTuAtom("true")), createTuCons( S("numbervars", createTuAtom("false")), createTuEmpty())));
        return write_term_3(stream_or_alias, out_term, options);
    }

    public boolean write_canonical_2(Term stream_or_alias, Term out_term) throws TuPrologError {
        initLibrary();
        Term options = createTuCons( S("quoted", createTuAtom("true")), TuFactory
                .createTuCons( S("ignore_ops", createTuAtom("true")), createTuCons( S("numbervars", createTuAtom("false")), createTuEmpty())));
        return write_term_3(stream_or_alias, out_term, options);
    }

    //per forzare il caricamento dell'I/O library 
    private void initLibrary() {
        if (flag == 1)
            return;

        TuLibrary library = null;

        library = engine.getLibrary("alice.tuprolog.lib.IOLibrary");
        if (library == null) {
            try {
                library = engine.loadLibrary("alice.tuprolog.lib.IOLibrary");
            } catch (InvalidLibraryException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                system_error(TuFactory.createTuAtom("IOLibrary does not exists."));
            }
        }

        IOLib = (IOLibrary) library;
        inputStream = IOLib.inputStream;
        outputStream = IOLib.outputStream;
        inputStreamName = IOLib.inputStreamName;
        outputStreamName = IOLib.outputStreamName;
        flag = 1;

        //inserisco anche stdin e stdout all'interno dell'hashtable con le sue propriet?
        Hashtable<String, Term> propertyInput = new Hashtable<String, Term>(10);
        inizialize_properties(propertyInput);
        propertyInput.put("input", createTuAtom("true"));
        propertyInput.put("mode", createTuAtom("read"));
        propertyInput.put("alias", createTuAtom("user_input"));
        //per essere coerente con la rappresentazione in IOLibrary dove stdin ? inputStreamName
        propertyInput.put("file_name", createTuAtom("stdin"));
        propertyInput.put("eof_action", createTuAtom("reset"));
        propertyInput.put("type", createTuAtom("text"));
        Hashtable<String, Term> propertyOutput = new Hashtable<String, Term>(10);
        inizialize_properties(propertyOutput);
        propertyOutput.put("output", createTuAtom("true"));
        propertyOutput.put("mode", createTuAtom("append"));
        propertyOutput.put("alias", createTuAtom("user_output"));
        propertyOutput.put("eof_action", createTuAtom("reset"));
        propertyOutput.put("file_name", createTuAtom("stdout"));
        propertyOutput.put("type", createTuAtom("text"));
        inputStreams.put(inputStream, propertyInput);
        outputStreams.put(outputStream, propertyOutput);

        return;
    }

    //serve per inizializzare la hashmap delle propriet?
    private boolean inizialize_properties(Hashtable<String, Term> map) {
        Term s = createTuEmpty();
        map.put("file_name", s);
        map.put("mode", s);
        map.put("input", createTuAtom("false"));
        map.put("output", createTuAtom("false"));
        map.put("alias", s);
        map.put("position", createTuInt(0));
        map.put("end_of_stream", createTuAtom("not"));
        map.put("eof_action", createTuAtom("error"));
        map.put("reposition", createTuAtom("false"));
        map.put("type", s);
        return true;
    }

    //funzioni ausiliarie per effettuare controlli sugli stream in ingresso e per
    //restituire lo stream aperto che sto cercando

    private InputStream find_input_stream(Term stream_or_alias) throws TuPrologError {
        int flag = 0;
        InputStream result = null;
        stream_or_alias = stream_or_alias.dref();
        Term stream = (Term) stream_or_alias;
        String stream_name = stream.fname();

        if (stream_or_alias.isVar()) { //controlla che non sia una variabile
            throw instantiation_error(engine.getEngineManager(), 1);
        }

        //Il nome del file che viene passato in input potrebbe essere il nome del file oppure il suo alias
        for (Map.Entry<InputStream, Hashtable<String, Term>> currentElement : inputStreams.entrySet()) {
            for (Map.Entry<String, Term> currentElement2 : currentElement.getValue().entrySet()) {

                //Puo' anche essere che l'utente inserisca il nome della variabile a cui e' associato lo stream che gli serve
                //in quel caso basta confrontare il nome dello stream con la chiave dell'elemento che sto analizzando (currentElement)
                if ((currentElement.getKey().toString()).equals(stream_name)) {
                    result = currentElement.getKey();
                    flag = 1;
                    break;
                } else if (currentElement2.getKey().equals("file_name")) {
                    if (stream_or_alias.equals(currentElement2.getValue())) {
                        result = currentElement.getKey();
                        flag = 1;
                        break;
                    }
                }
                //se mi viene passato un alias lo cerco e restituisco lo stream associato.
                else if (currentElement2.getKey().equals("alias")) {
                    Term alias = (Term) currentElement2.getValue();
                    int arity = alias.getPlArity();
                    //Ci posso essere anche piu' di un alias associti a quello stream, percio' devo controllare l'arita'
                    //della struttura che contiene tutti gli alias.
                    if (arity > 1) {
                        for (int k = 0; k < alias.getPlArity(); k++) {
                            if ((alias.getPlainArg(k)).equals(stream_or_alias)) {
                                result = currentElement.getKey();
                                flag = 1;
                                break;
                            }
                        }
                    } else {
                        //se arity e' uguale a 1, non devo fare un ciclo for, ha soltanto un elemento, percio' e' sufficiente fare alias.getName()
                        if (alias.fname().equals(stream_name)) {
                            result = currentElement.getKey();
                            flag = 1;
                            break;
                        }
                    }
                }
            }
        }

        //altrimenti vado a cercare lo stream nella hashtable.
        //Siccome gli stream di input o output possono essere invocati anche come "stdin" e "stdout"
        //faccio un controllo anche su quei nomi.
        if (stream_name.contains("Output") || stream_name.equals("stdout"))
            throw TuPrologError
                    .permission_error(engine.getEngineManager(), "output", "stream", stream_or_alias, createTuAtom("S_or_a is an output stream"));

        if (flag == 0)
            //se lo stream non si trova all'interno della hashtable, significa che non ? mai stato aperto
            throw existence_error(engine
                    .getEngineManager(), 1, "stream", stream_or_alias, createTuAtom("Input stream should be opened."));

        return result;
    }

    //stessa funzione di find_input_stream, ma sugli stream di output
    private OutputStream find_output_stream(Term stream_or_alias) throws TuPrologError {
        int flag = 0;
        OutputStream result = null;
        stream_or_alias = stream_or_alias.dref();
        Term stream = (Term) stream_or_alias;
        String stream_name = stream.fname();

        if (stream_or_alias.isVar()) {
            throw instantiation_error(engine.getEngineManager(), 1);
        }

        for (Map.Entry<OutputStream, Hashtable<String, Term>> currentElement : outputStreams.entrySet()) {
            for (Map.Entry<String, Term> currentElement2 : currentElement.getValue().entrySet()) {

                if ((currentElement.getKey().toString()).equals(stream_name)) {
                    result = currentElement.getKey();
                    flag = 1;
                    break;
                } else if (currentElement2.getKey().equals("file_name")) {
                    if (stream_or_alias.equals(currentElement2.getValue())) {
                        result = currentElement.getKey();
                        flag = 1;
                        break;
                    }
                }

                //se mi viene passato un alias lo cerco e restituisco lo stream associato.
                else if (currentElement2.getKey().equals("alias")) {
                    Term alias = (Term) currentElement2.getValue();
                    int arity = alias.getPlArity();
                    //Ci posso essere anche piu' di un alias associti a quello stream, percio' devo controllare l'arita'
                    //della struttura che contiene tutti gli alias.
                    if (arity > 1) {
                        for (int k = 0; k < alias.getPlArity(); k++) {
                            if ((alias.getPlainArg(k)).equals(stream_or_alias)) {
                                result = currentElement.getKey();
                                flag = 1;
                                break;
                            }
                        }
                    } else {
                        //se arity e' uguale a 1, non devo fare un ciclo for, ha soltanto un elemento, percio' e' sufficiente fare alias.getName()
                        if (alias.fname().equals(stream_name)) {
                            result = currentElement.getKey();
                            flag = 1;
                            break;
                        }
                    }
                }
            }
        }

        if (stream_name.contains("Input") || stream_name.equals("stdin"))
            throw TuPrologError
                    .permission_error(engine.getEngineManager(), "input", "stream", stream_or_alias, createTuAtom("S_or_a is an input stream."));

        if (flag == 0)
            //se lo stream non si trova all'interno della hashtable, significa che non ? mai stato aperto
            throw existence_error(engine
                    .getEngineManager(), 1, "stream", stream_or_alias, createTuAtom("Output stream should be opened."));

        return result;
    }

    //funzione che prende in ingresso lo stream e restituisce fine_name.
    //come nome dello stream viene utilizzata la proprieta' file_name
    private String get_output_name(OutputStream output) {
        Term file_name = null;
        //per reperire quello stream specifico devo per forza confrontare il nome degli stream ogni volta
        //perche' la get non fuziona in quanto non mi viene passato lo stesso oggetto 
        //che e' all'interno dell'hashtable dall'esterno, quindi devo trovarlo scorrendo ogni membro dell'Hashtable
        for (Map.Entry<OutputStream, Hashtable<String, Term>> element : outputStreams.entrySet()) {
            if ((element.getKey().toString()).equals(output.toString())) {
                Hashtable<String, Term> properties = element.getValue();
                file_name = properties.get("file_name");
                break;
            }
        }
        Term returnElement = (Term) file_name;
        return returnElement.fname();
    }

    private String get_input_name(InputStream input) {
        Term file_name = null;
        for (Map.Entry<InputStream, Hashtable<String, Term>> element : inputStreams.entrySet()) {
            if ((element.getKey().toString()).equals(input.toString())) {
                input = element.getKey();
                Hashtable<String, Term> properties = element.getValue();
                file_name = properties.get("file_name");
                break;
            }
        }
        Term returnElement = (Term) file_name;
        return returnElement.fname();
    }

    public boolean set_write_flag_1(Term number) throws TuPrologError {
        TuNumber n = (TuNumber) number;
        if (n.intValue() == 1) {
            write_flag = 1;
            return true;
        } else if (n.intValue() == 0) {
            write_flag = 0;
            return true;
        } else {
            throw domain_error(engine.getEngineManager(), 1, "0-1", number);
        }
    }
}