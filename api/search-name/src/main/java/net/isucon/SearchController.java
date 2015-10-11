package net.isucon;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@SuppressWarnings("unused")
public class SearchController {
    @RequestMapping(value = "/surname", method = RequestMethod.GET)
    public
    @ResponseBody
    Name[] searchSurname(@RequestParam(value = "q", required = true) String query) {
        return new Name[]{new Name("山田", "ヤマダ")};
    }

    @RequestMapping(value = "/givenname", method = RequestMethod.GET)
    public
    @ResponseBody
    Name[] searchGivenName(@RequestParam(value = "q", required = true) String query) {
        return new Name[]{new Name("太郎", "タロウ")};
    }
}
