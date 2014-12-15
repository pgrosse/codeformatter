package de.dbh.intellij.codeformatter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * User: pgr
 * Date: 06.06.2014
 */
public class Codeformatter {

    private final String[] schluesselwoerter = {"if", "for", "while"};


    public static void main(String[] args) {
        try {
//            new Codeformatter().testMethodCall();
            new Codeformatter().testVariable();
        } catch( IOException e ) {
            e.printStackTrace();
        }
    }


    public void testVariable() throws IOException {
//        String textToFormat = "    test.bla = 1;\n    test.blaa = 2;\n    int testVar = 3\n    public final int testVar2 = 4\n    public final static String testVar3 = 5\n";
        String textToFormat = "        test.bla = 1;\n        test.blaa = 2;\n";
        String formattedText = format( textToFormat );
        System.out.println( formattedText );
    }


    public void testMethodCall() throws IOException {
        String textToFormat = "        getPosPanel().getArtikelNrInput().addFocusAction(new LAction(this,\"checkArtikelNrInfoButtonEnabled\"));\n" +
                "        getPosPanel().getZollCodeInput().addFocusAction(new LAction(this,\"checkArtikelNrInfoButtonEnabled\"));\n" +
                "        getPosPanel().getMengeInput().addFocusAction(new LAction(this,\"checkArtikelMengeInfoButtonEnabled\"));\n" +
                "        getPosPanel().getZollCodeInput().addFocusAction(new LAction(this,\"checkZollCodeInfoButtonEnabled\"));\n";

        BufferedReader br               = new BufferedReader(new StringReader(textToFormat));
        List<Object>           lines    = new ArrayList<Object>();

        String zeile;

        // Laengen ermitteln
        while((zeile = br.readLine()) != null) {

            char[] chars = new char[zeile.length()];
            zeile.getChars( 0, zeile.length() - 1, chars, 0 );

            if(isMethodenaufruf( zeile )) {
//                List<Object> subsizes = new ArrayList<Object>();
//                Map<String, Object> content = analyseMethodCall( zeile, subsizes );
//                lines.add(new Object[]{ content, subsizes });
                lines.add( zeile );
            } else {
                lines.add( zeile );
            }

        }

        List<Object> sizes = new ArrayList<Object>();

        for(Object line : lines) {
            if(line instanceof Object[]) {
                Object[] lineElements = (Object[]) line;
                List<Object> linesizes = (List<Object>)lineElements[1];
                sizes = joinSizeLists( sizes, linesizes );
            }
        }


        for(Object line : lines) {
            if(line instanceof String) {
                System.out.println(line);
            } else {
                String outputLine = methodCallContentToString(line);
                System.out.println(outputLine);
            }
        }
    }


    private List<Object> joinSizeLists(List<Object> listToJoinTo, List<Object> listToJoin) {
        for(int i = 0; i < listToJoin.size(); i++) {
            Object lineElement = listToJoin.get(i);
            if(listToJoinTo.size() <= i) {
                listToJoinTo.add( lineElement );
            } else {
                Object element = listToJoinTo.get(i);

                if(element instanceof Integer) {
                    Integer lineElementSize = (Integer) lineElement;
                    Integer elementSize     = (Integer) element;
                    if(lineElementSize > elementSize) {
                        listToJoinTo.set( i, lineElementSize );
                    }
                }

                if(element instanceof List) {
                    List<Object> lineElementSizes = (List<Object>)lineElement;
                    List<Object> elementSizes = (List<Object>)element;

                    List<Object> joinedSizes = joinSizeLists( elementSizes, lineElementSizes );
                    listToJoinTo.set( i, joinedSizes );
                }
            }
        }
        return listToJoinTo;
    }


    private String methodCallContentToString(Object line) {
        return null;
    }


    public String format(String textToFormat) throws IOException {
        BufferedReader br               = new BufferedReader(new StringReader(textToFormat));
        int            maxvarlen        = 0;
        int            maxnamelen       = 0;
        List<Integer> breiten          = new ArrayList<Integer>();
        List<Integer>  parameterBreiten = new ArrayList<Integer>();

        List<Object>              lines    = new ArrayList<Object>();

        String zeile;

        // Laengen ermitteln
        while((zeile = br.readLine()) != null) {

            if(isZeileMitSchluesselwort(zeile)) {
                lines.add( zeile );
                continue;
            }

            if(!zeile.contains("=")) {
                breiten = aktualisiereMethodenaufrufBreiten(zeile, breiten);
                String innenteil = getParameter(zeile);
                parameterBreiten = aktualisiereParameterListenBreiten(innenteil, parameterBreiten);

                char[] chars = new char[zeile.length()];
                zeile.getChars( 0, zeile.length() - 1, chars, 0 );

                if(isMethodenaufruf( zeile )) {
                    List<Object> subsizes = new ArrayList<Object>();
                    Map<String, Object> content = analyseMethodCall( zeile, subsizes );
                    lines.add(new Object[]{ content, subsizes });
                } else {
                    lines.add( zeile );
                }

                continue;
            }

            int          pos            = zeile.indexOf("=");
            String       vorderteil     = zeile.substring(0,pos - 1);

            String[]     vorderteile    = getVorderteile( vorderteil );
            List<String> vorderteileLst = new ArrayList<String>(2);

            for(int i=0; i < vorderteile.length; i++) {
                if(vorderteile[i].length() > 0) {
                    vorderteileLst.add(vorderteile[i]);
                }
            }

            if( vorderteileLst.size() == 1) {
                if( vorderteileLst.get( 0 ).length() > maxnamelen ) {
                    maxnamelen = vorderteileLst.get( 0 ).length();
                }

                lines.add( zeile );
            }

            if(vorderteileLst.size() == 2) {

                if( vorderteileLst.get( 0 ).length() > maxvarlen ) {
                    maxvarlen = vorderteileLst.get( 0 ).length();
                }

                if( vorderteileLst.get( 1 ).length() > maxnamelen ) {
                    maxnamelen = vorderteileLst.get( 1 ).length();
                }

                lines.add( zeile );
            }
        }

        // Zeilen formattieren
//            br = new BufferedReader(new StringReader(textToFormat));
        StringBuffer formattierterText = new StringBuffer(65536);

        for(Object line : lines) {

            if(line instanceof String) {
                zeile = (String)line;

                if(isZeileMitSchluesselwort( zeile )) {
                    formattierterText.append( zeile );
                } else {
                    int          pos               = zeile.indexOf("=");
                    String       vorderteil        = zeile.substring(0, pos);
                    String[]     vorderteile       = getVorderteile( vorderteil );
                    List<String> vorderteileLst    = new ArrayList<String>(2);
                    boolean      firstFound        = false;
                    int          prefixWhitespaces = 0;

                    for(int i=0; i < vorderteile.length; i++) {
                        if(vorderteile[i].length() > 0) {
                            vorderteileLst.add(vorderteile[i]);
                            firstFound = true;
                        } else if(!firstFound) {
                            prefixWhitespaces++;
                        }
                    }

                    if(vorderteileLst.size() == 1) {
                        formattierterText.append( leerzeichen( prefixWhitespaces ) );
                        vorderteileLst.add( 0, leerzeichen( maxvarlen ) );
                    } else if(vorderteileLst.size() == 2) {
                        formattierterText.append( leerzeichen( prefixWhitespaces ) );
                    } else {
                        formattierterText.append(zeile);
                        formattierterText.append("\n");
                        continue;
                    }

                    String vartyp = vorderteileLst.get(0);

                    formattierterText.append(vartyp);

                    if(vartyp.length() < maxvarlen) {
                        formattierterText.append(leerzeichen(maxvarlen - vartyp.length()));
                    }

                    String varname = vorderteileLst.get(1);

                    if( maxvarlen > 0 && vartyp.trim().length() > 0) {
                        formattierterText.append( " " );
                    }

                    formattierterText.append(varname);

                    if(varname.length() < maxnamelen) {
                        formattierterText.append(leerzeichen(maxnamelen - varname.length()));
                    }

                    formattierterText.append(" ");
                    formattierterText.append(zeile.substring(pos, zeile.length()));
                    formattierterText.append("\n");
                }
            }

            if (line instanceof Object[]) {

                zeile = formatMethodenaufruf(zeile, breiten, parameterBreiten);
                formattierterText.append(zeile);
                formattierterText.append("\n");
                continue;
            }

        }

        return formattierterText.toString();
    }


    private String[] getVorderteile( String vorderteil ) {
        String[] vorderteile = vorderteil.split(" ");

        if(vorderteile.length > 2) {
            String[] korrigierteVorderteile = new String[] { "", "" };
            int prefixWhitespaces = 0;

            for( int i = 0; i < vorderteile.length - 1; i++ ) {

                if( vorderteile[i].length() == 0) {
                    prefixWhitespaces++;
                    continue;
                }

                if(korrigierteVorderteile[0] != null && korrigierteVorderteile[0].length() > 0 ) {
                    korrigierteVorderteile[0] += " ";
                }

                korrigierteVorderteile[0] += vorderteile[i];

            }

            korrigierteVorderteile[0] = leerzeichen( prefixWhitespaces ) + korrigierteVorderteile[0];
            korrigierteVorderteile[1] = vorderteile[ vorderteile.length - 1 ];
            vorderteile = korrigierteVorderteile;
        }

        return vorderteile;
    }


    private String formatMethodenaufruf(String text, List<Integer> breiten, List<Integer> parameterBreiten) {
        if(!isMethodenaufruf(text.trim())) {
            return text;
        }
//            if(!(text.contains("(") && text.endsWith(");"))) {
//                return text;
//            }

        String            vorderteil        = text.substring(0, text.indexOf("("));
        StringTokenizer st                = new StringTokenizer(vorderteil, ".");
        Iterator<Integer> breitenIt         = breiten.iterator();
        int               prefixWhitespaces = countLeadingWhitespaces(text);
        StringBuilder     formatierterText  = new StringBuilder(leerzeichen(prefixWhitespaces));

        while(st.hasMoreElements()) {
            String teil   = st.nextToken().trim();
            int    len    = teil.length();
            int    maxlen = breitenIt.next();

            formatierterText.append( teil );

            if(teil.length() < maxlen) {
                formatierterText.append( leerzeichen(maxlen - len) );
            }

            if(st.hasMoreElements()) {
                formatierterText.append( "." );
            }
        }

        if(breitenIt.hasNext()) {
            while(breitenIt.hasNext()) {
                formatierterText.append(leerzeichen(breitenIt.next()));
            }
        }

        String innenteil = text.substring(text.indexOf("(") + 1, text.lastIndexOf(")")).trim();

        formatierterText.append( "(" );

        if(innenteil.length() > 0) {
            if (!innenteil.startsWith(" ")) {
                formatierterText.append(" ");
            }

            if(innenteil.contains(",")) {
                innenteil = formatParameterListe(innenteil, parameterBreiten);
            }
            formatierterText.append( innenteil );

            if (!innenteil.endsWith(" ")) {
                formatierterText.append(" ");
            }
        }

        formatierterText.append( ");" );

        return formatierterText.toString();
    }


    private List<Integer> aktualisiereMethodenaufrufBreiten(String text, List<Integer> breiten) {
        if(!isMethodenaufruf(text.trim())) {
            return breiten;
        }
//            if(!(text.contains("(") && text.endsWith(");"))) {
//                return breiten;
//            }

        String            vorderteil = getMethode(text);
        StringTokenizer   st         = new StringTokenizer(vorderteil, ".");
        Iterator<Integer> breitenIt  = breiten.iterator();
        List<Integer>     neueBreiten = new ArrayList<Integer>();

        while(st.hasMoreElements()) {
            String teil = st.nextToken().trim();

            int len = teil.length();

            if(breitenIt.hasNext()) {
                int maxlen = breitenIt.next();

                if (len > maxlen) {
                    neueBreiten.add(len);
                } else {
                    neueBreiten.add(maxlen);
                }

            } else {
                neueBreiten.add(len);
            }
        }

        return neueBreiten;
    }


    private Map analyseMethodCall(String textToAnalyse, List<Object> sizes) {
        Map<String, Object> content = new HashMap<String, Object>();

        StringBuilder buffer = new StringBuilder();
        String        key    = null;

        char[] chars = new char[textToAnalyse.length()];
        textToAnalyse.getChars( 0, textToAnalyse.length(), chars, 0 );

        for(int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if(c == '(') {
                key   = buffer.toString();
                buffer = new StringBuilder();
            } else if(c == ')') {
                Object child;
                String value = buffer.toString();
                if(isMethodenaufruf( value )) {
                    List<Object> subsizes = new ArrayList<Object>();
                    child = analyseMethodCall( value, subsizes );
                    sizes.add( subsizes );
                } else if(isParameter( buffer.toString() )) {
                    List<Object> subsizes = new ArrayList<Object>();
                    child = splitKommaseperatedList( value,  subsizes);
                    sizes.add( subsizes );
                } else {
                    child = value;
                    sizes.add(value.length());
                }
                content.put(key, child);
                return content;
            } else {
                buffer.append( c );
            }
        }

        return content;
    }


    private List<Object> splitKommaseperatedList(String textToSplit, List<Object> sizes) {
        char[] chars = new char[textToSplit.length()];
        textToSplit.getChars( 0, textToSplit.length(), chars, 0 );

        boolean string   = false;
        boolean method   = false;
        List<Object> elements = new ArrayList<Object>();
        StringBuilder buffer = new StringBuilder();

        for(int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if(string) {
                buffer.append( c );
            }
            if(c == '\"') {
                string = !string;
            } else {
                if( c == '(' ) {
                    method = true;
                } else if( c == ')' ) {
                    List<Object> subsizes = new ArrayList<Object>();
                    Map element = analyseMethodCall( buffer.toString(), subsizes );
                    elements.add( element );
                    sizes.add(subsizes);
                    buffer = new StringBuilder();
                    method = false;
                } else if( !method && c == ',' ) {
                    String value = buffer.toString();
                    elements.add( value );
                    sizes.add(value.length());
                    buffer = new StringBuilder();
                } else {
                    buffer.append( c );
                }
            }
        }

        return elements;
    }

    private List<Integer> aktualisiereParameterListenBreiten(String text, List<Integer> breiten) {
        if(!isParameter(text)) {
            return breiten;
        }
//            if (!(text.contains(","))) {
//                return breiten;
//            }

        StringTokenizer st = new StringTokenizer(text, ",");
        Iterator<Integer> breitenIt = breiten.iterator();
        List<Integer> neueBreiten = new ArrayList<Integer>();

        while (st.hasMoreElements()) {
            String teil = st.nextToken().trim();

            int len = teil.length();

            if (breitenIt.hasNext()) {
                int maxlen = breitenIt.next();

                if (len > maxlen) {
                    neueBreiten.add(len);
                } else {
                    neueBreiten.add(maxlen);
                }

            } else {
                neueBreiten.add(len);
            }
        }

        return neueBreiten;
    }


    private String formatParameterListe(String text, List<Integer> breiten) {
        if(!isParameter(text)) {
            return text;
        }

        StringTokenizer   st                = new StringTokenizer(text, ",");
        Iterator<Integer> breitenIt         = breiten.iterator();
        StringBuilder     formatierterText  = new StringBuilder();

        while(st.hasMoreElements()) {
            String teil   = st.nextToken().trim();
            int    len    = teil.length();
            int    maxlen = breitenIt.next();

            formatierterText.append( teil );

            if(teil.length() < maxlen) {
                formatierterText.append( leerzeichen(maxlen - len) );
            }

            if(st.hasMoreElements()) {
                formatierterText.append( ", " );
            }
        }

        if(breitenIt.hasNext()) {
            while(breitenIt.hasNext()) {
                formatierterText.append(leerzeichen(breitenIt.next()));
            }
        }

        return formatierterText.toString();
    }


    private boolean isZeileMitSchluesselwort(String zeile) {
        for (String schluesselwort : schluesselwoerter) {
            if (zeile.contains(schluesselwort + "(") || zeile.contains(schluesselwort + " (")) {
                return true;
            }
        }
        return false;
    }


    private String leerzeichen(int anzahl) {
        StringBuilder leerstring = new StringBuilder(anzahl);
        for(int i = 0; i < anzahl; i++) {
            leerstring.append(" ");
        }
        return leerstring.toString();
    }


    private int countLeadingWhitespaces(String text) {
        int whitespaces = 0;
        for(int i = 0; i < text.length(); i++) {
            String zeichen = text.substring(i,i + 1);
            if(" ".equalsIgnoreCase(zeichen)) {
                whitespaces++;
            } else if("\t".equalsIgnoreCase(zeichen)) {
                whitespaces += 4;
            } else {
                break;
            }
        }
        return whitespaces;
    }


    private String getParameter(String text) {
        return text.substring(text.indexOf("(") + 1, text.lastIndexOf(")")).trim();
    }


    private String getMethode(String text) {
        return text.substring(0, text.indexOf("("));
    }


    private boolean isMethodenaufruf(String text) {
        return text.contains("(") && text.contains(");");
//            Pattern p = Pattern.compile("[\\s,\\t]*[\\w,\\d,.]*\\([\\d\\w,\\(,\\),\\.,\\s,\"]*,*\\);[\\D,\\W]*");
//            Matcher m = p.matcher(text);
//            return m.matches();
    }


    private boolean isParameter(String text) {
        Pattern p = Pattern.compile("([\\w,\\(,\\),\\.,\\s,\"]*(,|))*");
        Matcher m = p.matcher(text);
        return m.matches();
    }

}
