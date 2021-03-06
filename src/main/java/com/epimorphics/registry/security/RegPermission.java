/******************************************************************
 * File:        RegPermission.java
 * Created by:  Dave Reynolds
 * Created on:  3 Apr 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *****************************************************************/

package com.epimorphics.registry.security;

import static com.epimorphics.registry.security.RegAction.Grant;
import static com.epimorphics.registry.security.RegAction.Register;
import static com.epimorphics.registry.security.RegAction.StatusUpdate;
import static com.epimorphics.registry.security.RegAction.Update;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.shiro.authz.Permission;

import com.epimorphics.util.EpiException;

/**
 * Represents registry permission structure. The basic string form is:
 * <pre>
 *   action:/path
 * </pre>
 * Where the <em>action</em> can be a comma separated list of actions and the
 * <em>path</em> is register or register item relative to the root of the registry.
 * Permissions inherit down the path structure so that some granted:
 * <pre>
 *   update:/def/reg
 * </pre>
 * is also implicitly granted:
 * <pre>
 *   update:/def/reg/sub
 * </pre>
 * <p>
 * The set of actions is constrained to match a {@link RegAction}.
 * </p>
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class RegPermission implements Permission {

    protected Set<RegAction> actions;
    protected String path = "";
    protected String impliedPath;

    protected static Map<String, RegAction[]> roleAliases = new HashMap<String, RegAction[]>();
    static {
        roleAliases.put("Manager", new RegAction[]{Register, Update, StatusUpdate, Grant});
        roleAliases.put("Maintainer", new RegAction[]{Update, Grant});
        roleAliases.put("Authorized", new RegAction[]{Register, Update, StatusUpdate});
    }

    public RegPermission(String permission) {
        int split = permission.indexOf(':');
        if (split != -1) {
            actions = parseActions( permission.substring(0, split) );
            path = permission.substring(split+1).trim();
            checkImpliedPath();
        } else {
            actions = parseActions(permission);
        }
    }

    public RegPermission(String actions, String path) {
        this(parseActions(actions), path);
    }

    public RegPermission(RegAction action, String path) {
        this(singleAction(action), path);
    }

    public RegPermission(Set<RegAction> actions, String path) {
        this.actions = actions;
        this.path = path.trim();
        checkImpliedPath();
    }
    
    private void checkImpliedPath() {
        if (path.contains("/_")) {
            impliedPath = path.replace("/_", "/");
        }
    }

    private static Set<RegAction> singleAction(RegAction action) {
        Set<RegAction> actions = new HashSet<RegAction>(1);
        actions.add(action);
        return actions;
    }

    private static Set<RegAction> parseActions(String actionsStr) {
        if (actionsStr.trim().equals("*")) {
            return singleAction(RegAction.WildCard);
        }
        Set<RegAction> actions = new HashSet<RegAction>(5);
        for (String actionStr : actionsStr.split(",")) {
            if (roleAliases.containsKey(actionStr)) {
                for (RegAction action : roleAliases.get(actionStr)) {
                    actions.add(action);
                }
            } else {
                RegAction action = RegAction.forString(actionStr.trim());
                if (action != null) {
                    actions.add(action);
                } else {
                    throw new EpiException("Illegal action in permissions string: " + actionStr);
                }
            }
        }
        return actions;
    }


    public Set<RegAction> getActions() {
        return actions;
    }

    public void setActions(Set<RegAction> actions) {
        this.actions = actions;
    }

    public void addAction(RegAction action) {
        this.actions.add(action);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
        checkImpliedPath();
    }

    protected String getImpliedPath() {
        return impliedPath;
    }
    
    /**
     * Returns the part of p which is not covered by this permission.
     * If there is no match then this is simply p itself.
     * If p is fully granted then returns null.
     * Otherwise returns a new target permission that has the same path
     * but a reduced set of actions
     */
    public RegPermission residual(RegPermission p) {
        if ( ! pathCovered(p) ) return p;

        if (actions.contains(RegAction.WildCard) || actions.containsAll(p.actions)) {
            return null;
        }

        Set<RegAction> residualActions = new HashSet<RegAction>( p.actions );
        residualActions.removeAll(actions);
        return new RegPermission(residualActions, p.path);
    }

    @Override
    public boolean implies(Permission p) {
        if ( ! (p instanceof RegPermission) ) return false;
        RegPermission other = (RegPermission)p;

        if (actions.contains(RegAction.WildCard) || actions.containsAll(other.actions)) {
            if (pathCovered(other)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean pathCovered(RegPermission p) {
        if  (p.path.startsWith(path)) {
            return true;
        } else {
            if (impliedPath != null && p.path.startsWith(impliedPath)) {
                return true;
            }
        }
        return false;
    }
    
    public String getActionString() {
        StringBuffer buff = new StringBuffer();
        boolean started = false;
        for (RegAction action : actions) {
            if (started) {
                buff.append(",");
            } else {
                started = true;
            }
            buff.append(action.name());
        }
        return buff.toString();
    }

    @Override
    public String toString() {
        return getActionString() + ":" + path;
    }
}
