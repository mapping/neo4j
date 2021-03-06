/**
 * Copyright (c) 2002-2012 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.neo4j.helpers;

import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.neo4j.helpers.Functions.map;
import static org.neo4j.helpers.Settings.DURATION;
import static org.neo4j.helpers.Settings.INTEGER;
import static org.neo4j.helpers.Settings.MANDATORY;
import static org.neo4j.helpers.Settings.NO_DEFAULT;
import static org.neo4j.helpers.Settings.PATH;
import static org.neo4j.helpers.Settings.STRING;
import static org.neo4j.helpers.Settings.basePath;
import static org.neo4j.helpers.Settings.isFile;
import static org.neo4j.helpers.Settings.list;
import static org.neo4j.helpers.Settings.matches;
import static org.neo4j.helpers.Settings.max;
import static org.neo4j.helpers.Settings.min;
import static org.neo4j.helpers.Settings.range;
import static org.neo4j.helpers.Settings.setting;
import static org.neo4j.helpers.collection.MapUtil.stringMap;

import java.io.File;
import java.util.List;

import org.junit.Test;
import org.neo4j.graphdb.config.Setting;

public class SettingsTest
{
    @Test
    public void testInteger()
    {
        Setting<Integer> setting = setting( "foo", INTEGER, "3" );

        // Ok
        assertThat( setting.apply( map( stringMap( "foo", "4" ) ) ), equalTo( 4 ) );

        // Bad
        try
        {
            setting.apply( map( stringMap( "foo", "bar" ) ) );
            fail();
        }
        catch ( IllegalArgumentException e )
        {
            // Ok
        }
    }

    @Test
    public void testList()
    {
        Setting<List<Integer>> setting = setting( "foo", list( ",", INTEGER ), "1,2,3,4" );
        assertThat( setting.apply( map( stringMap() ) ).toString(), equalTo( "[1, 2, 3, 4]" ) );
    }

    @Test
    public void testMin()
    {
        Setting<Integer> setting = setting( "foo", INTEGER, "3", min( 2 ) );

        // Ok
        assertThat( setting.apply( map( stringMap( "foo", "4" ) ) ), equalTo( 4 ) );

        // Bad
        try
        {
            setting.apply( map( stringMap( "foo", "1" ) ) );
            fail();
        }
        catch ( IllegalArgumentException e )
        {
            // Ok
        }

    }

    @Test
    public void testMax()
    {
        Setting<Integer> setting = setting( "foo", INTEGER, "3", max( 5 ) );

        // Ok
        assertThat( setting.apply( map( stringMap( "foo", "4" ) ) ), equalTo( 4 ) );

        // Bad
        try
        {
            setting.apply( map( stringMap( "foo", "7" ) ) );
            fail();
        }
        catch ( IllegalArgumentException e )
        {
            // Ok
        }
    }

    @Test
    public void testRange()
    {
        Setting<Integer> setting = setting( "foo", INTEGER, "3", range( 2, 5 ) );

        // Ok
        assertThat( setting.apply( map( stringMap( "foo", "4" ) ) ), equalTo( 4 ) );

        // Bad
        try
        {
            setting.apply( map( stringMap( "foo", "1" ) ) );
            fail();
        }
        catch ( IllegalArgumentException e )
        {
            // Ok
        }

        try
        {
            setting.apply( map( stringMap( "foo", "6" ) ) );
            fail();
        }
        catch ( IllegalArgumentException e )
        {
            // Ok
        }
    }

    @Test
    public void testMatches()
    {
        Setting<String> setting = setting( "foo", STRING, "abc", matches( "a*b*c*" ) );

        // Ok
        assertThat( setting.apply( map( stringMap( "foo", "aaabbbccc" ) ) ), equalTo( "aaabbbccc" ) );

        // Bad
        try
        {
            setting.apply( map( stringMap( "foo", "cba" ) ) );
            fail();
        }
        catch ( IllegalArgumentException e )
        {
            // Ok
        }
    }

    @Test( expected = IllegalArgumentException.class )
    public void testDurationWithBrokenDefault()
    {
        // Notice that the default value is less that the minimum
        Setting<Long> setting = setting( "foo.bar", DURATION, "1s", min( DURATION.apply( "3s" ) ) );
        setting.apply( map( stringMap() ) );
    }

    @Test( expected = IllegalArgumentException.class )
    public void testDurationWithValueNotWithinConstraint()
    {
        Setting<Long> setting = setting( "foo.bar", DURATION, "3s", min( DURATION.apply( "3s" ) ) );
        setting.apply( map( stringMap( "foo.bar", "2s" ) ) );
    }

    @Test
    public void testDuration()
    {
        Setting<Long> setting = setting( "foo.bar", DURATION, "3s", min( DURATION.apply( "3s" ) ) );
        assertThat( setting.apply( map( stringMap( "foo.bar", "4s" ) ) ), equalTo( 4000L ) );
    }

    @Test
    public void testDefault()
    {
        Setting<Integer> setting = setting( "foo", INTEGER, "3" );

        // Ok
        assertThat( setting.apply( map( stringMap() ) ), equalTo( 3 ) );
    }

    @Test
    public void testMandatory()
    {
        Setting<Integer> setting = setting( "foo", INTEGER, MANDATORY );

        // Check that missing mandatory setting throws exception
        try
        {
            setting.apply( map( stringMap() ) );
            fail();
        }
        catch ( Exception e )
        {
            // Ok
        }
    }

    @Test
    public void testPaths()
    {
        Setting<File> home = setting( "home", PATH, "." );
        Setting<File> config = setting( "config", PATH, "config.properties", basePath( home ), isFile );
        assertThat( config.apply( map( stringMap() ) ).getAbsolutePath(),
            equalTo( new File( ".", "config.properties" ).getAbsolutePath() ) );
    }

    @Test
    public void testInheritOneLevel()
    {
        Setting<Integer> root = setting( "root", INTEGER, "4" );
        Setting<Integer> setting = setting( "foo", INTEGER, root );

        // Ok
        assertThat( setting.apply( map( stringMap( "foo", "1" ) ) ), equalTo( 1 ) );
        assertThat( setting.apply( map( stringMap() ) ), equalTo( 4 ) );
    }

    @Test
    public void testInheritHierarchy()
    {
        // Test hierarchies
        Setting<String> a = setting( "A", STRING, "A" ); // A defaults to A
        Setting<String> b = setting( "B", STRING, "B", a ); // B defaults to B unless A is defined
        Setting<String> c = setting( "C", STRING, "C", b ); // C defaults to C unless B is defined
        Setting<String> d = setting( "D", STRING, b ); // D defaults to B
        Setting<String> e = setting( "E", STRING, d ); // E defaults to D (hence B)

        assertThat( c.apply( map( stringMap( "C", "X" ) ) ), equalTo( "X" ) );
        assertThat( c.apply( map( stringMap( "B", "X" ) ) ), equalTo( "X" ) );
        assertThat( c.apply( map( stringMap( "A", "X" ) ) ), equalTo( "X" ) );
        assertThat( c.apply( map( stringMap( "A", "Y", "B", "X" ) ) ), equalTo( "X" ) );

        assertThat( d.apply( map( stringMap() ) ), equalTo( "B" ) );
        assertThat( e.apply( map( stringMap() ) ), equalTo( "B" ) );

    }

    @Test( expected = IllegalArgumentException.class )
    public void testMandatoryApplyToInherited()
    {
        // Check that mandatory settings fail even in inherited cases
        Setting<String> x = setting( "X", STRING, NO_DEFAULT );
        Setting<String> y = setting( "Y", STRING, MANDATORY, x );

        y.apply( Functions.<String, String>nullFunction() );
    }
}
