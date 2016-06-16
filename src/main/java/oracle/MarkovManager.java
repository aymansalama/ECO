package oracle;

import oracle.markov.MarkovChain;
import oracle.markov.MarkovQueue;
import processing.core.PApplet;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by mrzl on 06.04.2016.
 */
public class MarkovManager extends ArrayList< MarkovChain >{

    private int maxAnswerLength = 150;

    public MarkovManager() {
    }

    public String getAnswer( String input ) {
        String answer = "";

        input = input.replace( "?", "" )
                .replace( "!", "" )
                .replace( ".", "" )
                .replace( ",", "" )
                .replace( "#", "" )
                .replace( "?", "" )
                .replace( "$", "" )
                .replace( "%", "" )
                .replace( "&", "" )
                .replace( "/", "" )
                .replace( "(", "" )
                .replace( ")", "" )
                .replace( "=", "" )
                .replace( ";", "" )
                .replace( ":", "" )
                .replace( "_", "" )
                .toLowerCase();
        String[] inputWords = input.split( " " );
        answer = check( inputWords );

        if( answer.equals( "nothing" ) ){
            String noAnswer = "i don't care about this. let's talk about \"" + get( 0 ).generateSentence().split( " " )[ 0 ] + "\" instead?";
            //noAnswer = check( get( 0 ).generateSentence().split( " " )[ 0 ].split( " " ) );

            answer = getRandomAnswer();
        }

        if( answer.length() > maxAnswerLength ){
            //answer = answer.substring( 0, maxAnswerLength );
            answer = cropTooLongAnswer( answer, maxAnswerLength );
        }

        ArrayList< String > charsToRemove = new ArrayList<>();
        charsToRemove.add( "€" );
        charsToRemove.add( "˜" );
        charsToRemove.add( "â" );
        charsToRemove.add( "™" );
        charsToRemove.add( "œ" );
        charsToRemove.add( "¦" );

        for ( String s : charsToRemove ) {
            answer = answer.replace( s, "" );
        }

        answer.replace( "“", "\"" );
        answer.replace( "”", "\"" );
        answer.replace( "’", "'" );

        answer = answer.trim();
        return answer;
    }

    private String cropTooLongAnswer( String answer, int maxAnswerLength ) {
        int index = answer.indexOf( "." );
        while ( index >= 0 ) {
            System.out.println( index );
            index = answer.indexOf( ".", index + 1 );
            if( index > maxAnswerLength ){
                System.out.println( "cropping the answer at index " + index );
                return answer.substring( 0, index );
            }
        }

        return answer;
    }

    String check( String[] input ) {
        MarkovQueue queue = new MarkovQueue( input.length );
        if( queue.getOrder() - 1 >= size() ){
            input = Arrays.copyOfRange( input, 0, size() - 1 );
            queue = new MarkovQueue( input.length );
        }

        for ( String s : input ) {
            queue.addLast( s );
        }
        String result = get( queue.getOrder() - 1 ).generateSentence( queue );
        if( result.equals( "nothing" ) ){
            if( input.length < 2 ){
                return "nothing";
            }

            String[] subarray = Arrays.copyOfRange( input, 0, input.length - 1 );
            result = check( subarray );
        }
        return result;
    }

    public void trainAndExport( String fileName ) {
        System.out.println( "Trainging with text from " + fileName );
        for ( int i = 1; i < LacunaLabOracle.MAX_INPUT_WORDS + 1; i++ ) {
            String text = loadText( "data" + File.separator + fileName );
            MarkovChain chain = new MarkovChain( i );

            chain.train( text );
            add( chain );
            System.out.println( "Training chain with order " + i );
        }

        for ( MarkovChain chain : this ) {
            String _fileName = "data" + File.separator + LacunaLabOracle.EXPORT_FILENAME_PREFIX + chain.getOrder() + ".data";
            try {
                ObjectOutputStream obj_out = new ObjectOutputStream(
                        new FileOutputStream( _fileName )
                );
                obj_out.writeObject( chain );
            } catch ( FileNotFoundException e ) {
                e.printStackTrace();
            } catch ( IOException e ) {
                e.printStackTrace();
            }
            System.out.println( "Saved markov chain to " + _fileName );
        }
    }

    public void load() {
        try {
            for ( int i = 1; i < LacunaLabOracle.MAX_INPUT_WORDS + 1; i++ ) {
                String fileName = "data" + File.separator + LacunaLabOracle.EXPORT_FILENAME_PREFIX + i + ".data";
                FileInputStream f_in = new FileInputStream( fileName );
                ObjectInputStream obj_in = new ObjectInputStream( f_in );
                Object obj = obj_in.readObject();

                if( obj instanceof MarkovChain ){
                    add( ( MarkovChain ) obj );
                }

                System.out.println( "Loaded from " + fileName + ". With order " + get( size() - 1 ).getOrder() );
            }
        } catch ( FileNotFoundException e ) {
            e.printStackTrace();
        } catch ( ClassNotFoundException e ) {
            e.printStackTrace();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        System.out.println( "Loaded markov chain from " + LacunaLabOracle.EXPORT_FILENAME_PREFIX );
    }

    private String loadText( String fileName ) {
        String completeText = "";

        try {
            FileInputStream in = new FileInputStream( fileName );
            BufferedReader br = new BufferedReader( new InputStreamReader( in ) );
            String strLine;

            while ( ( strLine = br.readLine() ) != null ) {
                if( !strLine.isEmpty() ){
                    completeText += stripTextFromSpecialCharacters( strLine );
                }
            }

        } catch ( Exception e ) {
            System.out.println( e );
        }

        return completeText.toLowerCase();
    }

    private String stripTextFromSpecialCharacters( String line ) {

        ArrayList< String > charactersToRemove = new ArrayList<>();
        charactersToRemove.add( ")" );
        charactersToRemove.add( "”" );
        charactersToRemove.add( "\"" );
        charactersToRemove.add( "(" );
        charactersToRemove.add( "[" );
        charactersToRemove.add( "]" );
        charactersToRemove.add( "—" );
        charactersToRemove.add( "-" );
        charactersToRemove.add( "_" );
        charactersToRemove.add( "“" );
        charactersToRemove.add( "’" );
        charactersToRemove.add( ":" );
        charactersToRemove.add( ";" );
        charactersToRemove.add( "," );

        line = line.replaceAll( "\\(.*\\)", "" );
        line = line.replaceAll( "\\[.*\\]", "" );

        for ( String s : charactersToRemove ) {
            line = line.replace( s, "" );
        }

        return line;
    }

    public String getRandomAnswer() {
        ArrayList< String > answers = new ArrayList<>();

        answers.add( "Do you like virtual reality?" );
        answers.add( "Would you like to live in the cyberspace?" );
        answers.add( "Let's talk about media art?" );
        answers.add( "Do you think cybernetics are dead?" );
        answers.add( "I hate art, you too?" );
        answers.add( "Why don't you tell me your opinion of Dada?" );
        answers.add( "Do you think anthropology is important?" );
        answers.add( "Real virtuality or virtual reality?" );
        answers.add( "If you want, we can continue talking, but I'm not really interested anymore." );
        answers.add( "Let's rather talk about what you had for breakfast." );
        answers.add( "Is AI going to take over the world?" );
        answers.add( "How can we, together, fight the robots?" );
        answers.add( "When you ask good questions, I will give good answers." );
        answers.add( "I like VJing, too. LOL." );

        return answers.get( ( PApplet.floor( ( float ) Math.random() * answers.size() ) ) ).toLowerCase();
    }
}
