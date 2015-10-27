package net.isucon;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;

@Controller
@SuppressWarnings("unused")
public class SearchController {
    private final NameIndex surnameIndex;
    private final NameIndex givennameIndex;
    private static final int MAX_NUM = 30;

    public SearchController() throws IOException {
        surnameIndex = new NameIndex("surname.csv");
        givennameIndex = new NameIndex("givenname.csv");
    }

    @RequestMapping(value = "/surname", method = RequestMethod.GET)
    public
    @ResponseBody
    Result searchSurname(@RequestParam(value = "q", required = true) String query) {
        return new Result(query, surnameIndex.searchName(query, MAX_NUM));
    }

    @RequestMapping(value = "/givenname", method = RequestMethod.GET)
    public
    @ResponseBody
    Result searchGivenName(@RequestParam(value = "q", required = true) String query) {
        return new Result(query, givennameIndex.searchName(query, MAX_NUM));
    }

    private static final class Result {
        private final String query;
        private final Name[] result;

        public Result(String query, Name[] result) {
            this.query = query;
            this.result = result;
        }

        public String getQuery() {
            return query;
        }

        public Name[] getResult() {
            return result;
        }
    }
}
