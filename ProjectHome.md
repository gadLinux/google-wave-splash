Google Wave Splash is a lightweight, fast alternative client for Google Wave which is designed to work in a variety of browsers including IE6, mobile and others. It is a Java servlet application that can be run on any workstation, or embedded as a library in any type of Java application itself to fetch and render waves.

Google Wave Splash is entirely open source and is built on jquery and Wave Data APIs. Works (via OAuth) with Google Wave consumer instance and soon-to-be-Apache open source Wave implementation.

# Requirements #

You will need the following tools:

  * Apache Ant (1.7 or later)
  * Java SDK (1.6 or later)
  * Subversion (to access this repository)
  * OAuth

Splash uses OAuth to access the Google Wave consumer backends and provide live user data. In order to use this, OAuth requires a Key and Secret (read more about OAuth here). You can obtain these by registering a Google Wave robot. You don't actually need to create a Robot, rather just follow this process to obtain OAuth credentials.

Once you've got this, in your home directory create a file called `splash.properties`. My home directory on Linux is at `/home/dhanji`, on Mac it's `/Users/dhanji`. The file should look as follows

```
splash.oauth.key=...
splash.oauth.secret=...
```

Remember to fill in your OAuth key and secret here!

# Building and Running Splash #

First check out splash from the Subversion repository on Google Code:

```
hg clone https://splash.wave-protocol.googlecode.com/hg/ splash
svn checkout https://google-wave-splash.googlecode.com/svn/trunk/ google-wave-splash --username yourusername@gmail.com
cd splash
```

To run, use Ant:

```
ant run
```

And you're off to the races!

Visit http://localhost:8080/wave in a browser window to get to the initial OAuth login step. Here you will be prompted to provide access to your Google Wave account. Click Grant Access and continue to Splashy goodness!