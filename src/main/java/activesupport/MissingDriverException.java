package activesupport;

public class MissingDriverException extends Exception {

        public MissingDriverException(){
            super("[ERROR] Webdriver is missing." + "/n" + "[OPTIONS] install webdriver locally.");
        }
}
