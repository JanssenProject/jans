package io.jans.casa.ui.vm.admin;

import io.jans.casa.core.UserService;
import io.jans.casa.ui.UIUtils;
import io.jans.casa.misc.Utils;
import io.jans.casa.ui.model.PersonSearchMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Checkbox;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author jgomer
 */
public class PreferenceResetViewModel extends MainViewModel {

    private static final int MINLEN_SEARCH_PATTERN = 3;

    private Logger logger = LoggerFactory.getLogger(getClass());

    @WireVariable
    private UserService userService;

    private String searchPattern;
    private List<PersonSearchMatch> users;

    public String getSearchPattern() {
        return searchPattern;
    }

    public void setSearchPattern(String searchPattern) {
        this.searchPattern = searchPattern;
    }

    public List<PersonSearchMatch> getUsers() {
        return users;
    }

    @Init(superclass = true)
    public void childInit() { }

    public void search() {

        if (Utils.isNotEmpty(searchPattern)) {
            //Validates if input conforms to requirement of length
            if (searchPattern.trim().length() < MINLEN_SEARCH_PATTERN) {
                UIUtils.showMessageUI(Clients.NOTIFICATION_TYPE_WARNING, Labels.getLabel("adm.resets_textbox_hint", new Integer[]{MINLEN_SEARCH_PATTERN}));
            } else {
                users = userService.searchUsers(searchPattern.trim()).stream() //avoid UI cheaters by trimming
                        .map(person -> {
                            PersonSearchMatch p = new PersonSearchMatch();
                            p.setGivenName(person.getGivenName());
                            p.setLastName(person.getSurname());
                            p.setUserName(person.getUid());
                            p.setId(person.getInum());
                            return p;
                        }).sorted(Comparator.comparing(PersonSearchMatch::getUserName)).collect(Collectors.toList());

                //triggers update of interface
                BindUtils.postNotifyChange(this, "users");
            }
        }

    }

    @NotifyChange({"users"})
    public void doReset() {

        //Pick those that haven't been reset before and that are checked in the grid currently
        List<String> userInums = users.stream().filter(u -> !u.isAlreadyReset() && u.isChecked())
                .map(PersonSearchMatch::getId).collect(Collectors.toList());

        if (userInums.size() > 0) { //proceed only if there is some fresh selection in the grid
            //Perform the actual resetting
            int total = userService.resetPreference(userInums);
            boolean success = total == userInums.size();

            if (success) {      //Check the no. of users changed matches the expected
                users.forEach(usr -> usr.setAlreadyReset(usr.isChecked()));
                UIUtils.showMessageUI(true);
            } else {
                //Flush list if something went wrong
                users = null;
                String msg = Labels.getLabel("adm.resets_only_updated", new Integer[] { total });
                UIUtils.showMessageUI(false, Labels.getLabel("general.error.detailed", new String[] { msg }));
            }
            logActionDetails(Labels.getLabel("adm.resets_action"), success);

        } else {
            UIUtils.showMessageUI(false, Labels.getLabel("adm.resets_noselection"));
        }

    }

    //This simulates a click on a checkbox (although the click is coming from one made upon a row)
    public void rowClicked(Checkbox box, PersonSearchMatch user) {

		if (!box.isDisabled()) {
			//Simulate check on the checkbox
			box.setChecked(!box.isChecked());
			//Sync the user paired to this checkbox
			user.setChecked(box.isChecked());
		}

    }

    @NotifyChange({"users", "searchPattern"})
    public void cancelReset() {
        //Provoke the grid to disappear, and cleaning the search textbox
        users = null;
        searchPattern = null;
    }

}
