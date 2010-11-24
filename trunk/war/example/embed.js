// Copyright 2009 Google Inc. All Rights Reserved

/**
 * @fileoverview Wave Embed API
 *
 * @author Douwe Osinga
 * @author Rodrigo Damazio
 * @author Dhanji R. Prasanna <dhanji@gmail.com>
 */

// Safety net in case we're used outside google.load()
if (!('google' in window)) {
  window.google = {};
}
if (!('wave' in window.google)) {
  window.google.wave = {};
}

/**
 * The default wave application server URL.
 * @type {String}
 */
var WAVE_ROOT_URL = 'https://wave.google.com/wave/';

/**
 * The gadget rpc library to load to talk to the embedded frame
 * @type {String}
 */
var GADGET_RPC_LIB = 'https://wave.google.com/gadgets/js/core:rpc.js?debug=1&c=1';

// Global identifier (index) for each wave panel. Each panel has a unique id
// that is used to access it's frame
var WAVEPANEL_nextId = 0;

/**
 * The default UI configuration if one is not specified by the user.
 */
var DEFAULT_CONFIG = {
  bgcolor: "white",
  color: "black",
  font: "Arial",
  fontSize: "8pt",
  header: false,
  footer: false,
  toolbar: false,
  rootUrl: WAVE_ROOT_URL,
  width: 481,
  height: 500,
  lite: false
};

if (typeof gadgets == 'undefined' || !gadgets.rpc) {
  document.write('<script src="' + GADGET_RPC_LIB + '" ' +
      'type="text/javascript"></script>');
}

/**
 *
 * Constructs a <code>WavePanel</code> in the passed container.
 *
 * An <code>&lt;iframe&gt;</code> is created inside the supplied container;
 * if the container is null, the <code>&lt;iframe&gt;</code> is instead
 * appended to the current document.
 *
 * The created object can be used to interact with the panel and
 * indirectly with any displayed wave.
 *
 * @export
 * @constructor
 * @class This class defines an object to hold the embedded wave,
 * in which an <code>&lt;iframe&gt;</code> is constructed.
 * @param {String} [opt_config] A configuration object that contains
 *     several options for embedding. You may set css properties for
 *     <code>bgcolor, color</code>. Or whether display
 *     components <code>header, footer, toolbar</code> are enabled
 *     (true). You can also set the server URL via <code>rootUrl</code>.
 *     All attributes are optional as is the param itself.
 *
 */
window.google.wave.WavePanel = function(opt_config) {
  this.id_ = WAVEPANEL_nextId++;
  this.frameId_ = "iframe_panel_" + this.id_;
  this.eventListeners_ = {};
  this.init_ = false;

  if (opt_config) {
    // We keep this for backwards compatibility, opt_config as string
    // used to specify the server.
    if (typeof(opt_config) == 'string') {
      this.uiConfig_ = DEFAULT_CONFIG;
      this.waveRootUrl = opt_config;
    } else {
      this.uiConfig_ = {};
      this.waveRootUrl = opt_config.rootUrl || WAVE_ROOT_URL;

      // Fill in missing properties with defaults
      var prop;
      for (prop in DEFAULT_CONFIG) {
        this.uiConfig_[prop] = opt_config[prop] || DEFAULT_CONFIG[prop];
      }

      // If the user has given us a target div, use that.
      if (opt_config.target) {
        this.uiConfig_.target = opt_config.target;
      }

      if (opt_config.ownerAddress) {
        this.uiConfig_.ownerAddress = opt_config.ownerAddress;
      }
    }
  } else {
    this.uiConfig_ = DEFAULT_CONFIG;
    this.waveRootUrl = WAVE_ROOT_URL;
  }
}

/**
 * Returns the Id of this frame
 *
 * @return {Number} The Id of this frame.
 * @export
 */
window.google.wave.WavePanel.prototype.getId = function() {
  return this.id_;
};

/**
 * Actually creates the wave <code>&lt;iframe&gt;</code> inside
 * the given container. This creation is not done in the constructor
 * to allow the caller to set various initialization options.
 *
 * @param {Object} container to put the wavepanel IFrame in. IFrame will be
 *        appended to the body if null.
 * @param {Function} opt_loadCallback the callback to call after loading the
 *        wave iframe is done (optional).
 * @export
 */
window.google.wave.WavePanel.prototype.init = function(container, opt_loadCallback) {
  this.init_ = true;
  if (this.initWaveId_ && this.initSearch_) {
    throw 'Both an initial wave ID and a search were specified';
  }
  // Detect IE or requested option and display the light client instead.
  if (this.uiConfig_.lite || navigator && navigator.userAgent.indexOf('MSIE') != -1) {
    this.waveRootUrl = this.waveRootUrl + "w/";
    this.createLiteFrame_(container);
    return;
  }

  // Register RPC calls
  this.setupRpc_(opt_loadCallback);

  // Create the iframe with the initial actions
  this.createFrame_(container);

  // Attach the RPC to the relaying URL
  gadgets.rpc.setRelayUrl(this.getFrameId(), this.getRelayUrl_(), false);

  // Throw away initial action values
  delete this['initWaveId_'];
};

/**
 * Whether the rpc system has been initialized.
 * @type {boolean}
 */
window.google.wave.WavePanel.rpcSetup_ = false;

/**
 * See below, all pending call backs.
 * @type {Object}
 */
window.google.wave.WavePanel.callbackQueue_ = {};

/**
 * See below, the next call back id to use.
 * @type {number}
 */
window.google.wave.WavePanel.nextCallbackId = 0;

/**
 * Class method to push a callback on the queue.
 *
 * @param {Function} opt_callback to callback when done (optional).
 *        If no callback is specified, this method becomes a NOOP.
 * @return {String} an identifier that we later can use to
 *         find the callback back.
 * @private
 */
window.google.wave.WavePanel.pushCallback_ = function(opt_callback) {
  if (!opt_callback) {
    return '';
  }
  var id = '' + window.google.wave.WavePanel.nextCallbackId;
  window.google.wave.WavePanel.nextCallbackId++;
  window.google.wave.WavePanel.callbackQueue_[id] = opt_callback;
  return id;
};

/**
 * Class method to pop a callback off the queue.
 *
 * @param {String} callbackId is a previously returned id.
 * @return {Function} the associated callback.
 * @private
 */
window.google.wave.WavePanel.popCallback_ = function(callbackId) {
  if (callbackId == '') {
    return null;
  }
  var callback = window.google.wave.WavePanel.callbackQueue_[callbackId];
  delete window.google.wave.WavePanel.callbackQueue_[callbackId];
  return callback;
};

/**
 * Setup the rpc mechanism. Since this has to happen only once, we use the
 * static rpcSetup_ to see if we've already been here.
 *
 * @param {Function} opt_loadCallback the optional callback for when the client
 *        is done loading (optional).
 * @private
 */
window.google.wave.WavePanel.prototype.setupRpc_ = function(opt_loadCallback) {
  if (this.rpcSetup_) {
    return;
  }
  this.rpcSetup_ = true;

  var panel = this;
  gadgets.rpc.register('load_done', function() {
    if (opt_loadCallback) {
      opt_loadCallback();
    }
  });

  var processCallback = function(obj) {
    // The rpc mechanism returns the parameters in this['a'].
    // the first parameter is the callback-id, the second one
    // the value.
    // TODO(Douwe): Support more than one return value.
    var callback = window.google.wave.WavePanel.popCallback_(obj['a'][0]);
    if (callback) {
      callback(obj['a'][1]);
    }
  };

  gadgets.rpc.register('load_wave_done', function() {
    processCallback(this);
  });

  gadgets.rpc.register('digest_search_done', function() {
    processCallback(this);
  });

  gadgets.rpc.register('request_profiles', function() {
    if (!panel.profileProvider_) {
      throw 'Got a profiles request but no profile provider is set.';
    }
    panel.profileProvider_(this['a'][0]);
  });

  // Events include:
  // - request to add a new participant: 'request_add_participant'
  gadgets.rpc.registerDefault(function() {
    var eventType = this['s'];
    var eventObj = this['a'][0];
    var handlers = panel.eventListeners_[eventType];
    if (handlers) {
      for (var handler in handlers) {
        handlers[handler](eventObj);
      }
    }
  });
};


/**
 * Create the html node (iframe) for the window.google.wave.WavePanel.
 *
 * IFrames are used to allow us to be server directly from the gwave.com domain
 * for efficient browser channel based two way communicaiton.
 *
 * @param {Element} container Container element (typically a div) to place
 *        The panel in. If null, chart will be added to the page body.
 * @private
 */
window.google.wave.WavePanel.prototype.createFrame_ = function(container) {
  // Create the IFRAME.
  // The named IFrame has to be created by setting its HTML because IE bans
  // changing/setting the name attribute of existing IFrame element.
  var frameDiv = document.createElement('div');
  frameDiv.innerHTML = '<iframe name="' + this.frameId_ + '" >';
  var frame = frameDiv.firstChild;

  frame.id = this.frameId_;
  frame.width = "100%";
  frame.height = "100%";
  frame.frameBorder = 'no';
  frame.scrolling = 'no';
  frame.marginHeight = 0;
  frame.marginWidth = 0;
  frame.className = 'embed-iframe';

  // Create in specified div, or if none, in main body
  container = container || document.body;
  container.appendChild(frame);

  this.uiConfig_.width = container.clientWidth;
  this.uiConfig_.height = container.clientHeight - 80;

  frame.src = this.iframeUrl_();

  return frame;
};

/**
 * Create the html node (iframe) for the light client.
 *
 * @param {Element} container Container element (typically a div) to place
 *        The panel in. If null, wave will be added to the page body.
 * @private
 */
window.google.wave.WavePanel.prototype.createLiteFrame_ = function(container) {
  // Create the IFRAME.
  // The named IFrame has to be created by setting its HTML because IE bans
  // changing/setting the name attribute of existing IFrame element.
  var frameDiv = document.createElement('div');
  frameDiv.innerHTML = '<iframe name="' + this.frameId_ + '" >';
  var frame = frameDiv.firstChild;

  frame.id = this.frameId_;
  frame.width = "100%";
  frame.height = "100%";
  frame.frameBorder = 'no';
  frame.marginHeight = 0;
  frame.marginWidth = 0;
//  frame.className = 'embed-iframe';
  frame.src = this.iframeUrl_();

  // Create in specified div, or if none, in main body
  container = container || document.body;
  container.appendChild(frame);

  return frame;
};

/**
 * return the iframeUrl with an optional initial wave.
 *
 * @return {String} the url for the iframe.
 * @private
 */
window.google.wave.WavePanel.prototype.iframeUrl_ = function() {
  var resParams = [];
  resParams.push('client.type=embedded');
  resParams.push('parent=' +
      escape(window.location.protocol + '//' + window.location.host +
             window.location.pathname));
  if (this.authToken_) {
    resParams.push('auth=' + encodeURIComponent(this.authToken_));
  }
  if (this.initWaveId_) {
    resParams.push('wave_id=' + encodeURIComponent(this.initWaveId_));
  }
  if (this.uiConfig_.ownerAddress) {
    resParams.push('embed_owner_address='
        + encodeURIComponent(this.uiConfig_.ownerAddress));
    resParams.push('embed_owner_url=' + encodeURIComponent(parent.location));
  }
  if (this.uiConfig_) {
    var uiConfig = this.uiConfig_;
    resParams.push('bgcolor=' + encodeURIComponent(uiConfig.bgcolor));
    resParams.push('color=' + encodeURIComponent(uiConfig.color));
    resParams.push('font=' + encodeURIComponent(uiConfig.font));
    resParams.push('fontsize=' + encodeURIComponent(uiConfig.fontSize));
    resParams.push('embed_header=' + uiConfig.header);
    resParams.push('embed_footer=' + uiConfig.footer);
    resParams.push('embed_toolbar=' + uiConfig.toolbar);
    resParams.push('width=' + uiConfig.width + "px");
    resParams.push('height=' + uiConfig.height + "px");
  }
  return this.waveRootUrl + '?' + resParams.join('&');
};

/**
 * Adds an event listener.
 *
 * @param {String} eventType The name of the event to listen to.
 *        For a list of all possible events, see the embed documentation.
 *        TODO(douwe): once this documentation is up on code.google.com,
 *        add reference here.
 * @param {Function} handlerFunction A function that will be called when the
 *        specific event occurs. The function will receive a single event
 *        parameter.
 * @export
 */
window.google.wave.WavePanel.prototype.addListener = function(eventType, handlerFunction) {
  var allListeners = this.eventListeners_;
  var eventListeners = allListeners[eventType];
  if (!eventListeners) {
    eventListeners = [];
    allListeners[eventType] = eventListeners;
  }
  eventListeners.push(handlerFunction);
};

/**
 * Returns the frame ID.
 *
 * @return {String} the frame ID.
 * @export
 */
window.google.wave.WavePanel.prototype.getFrameId = function() {
  return this.frameId_;
};

/**
 * Returns the the RPC relay URL for this wave.
 *
 * @return {String} the url for the RPC relay.
 * @private
 */
window.google.wave.WavePanel.prototype.getRelayUrl_ = function() {
  var host = this.waveRootUrl;
  var parts = host.split('://');
  if (parts && parts.length > 0) {
    var s = parts[parts.length - 1];
    host = parts[0] + '://';
    for (var i = 0; i < s.length; ++i) {
      if (s[i] == '/') {
        break;
      }
      host += s[i];
    }
    host += '/';
  }
  return host + 'gadgets/files/container/rpc_relay.html';
};

// Actual RPC calls

/**
 * Loads a new wave into the <code><b>window.google.wave.WavePanel</b></code>.
 * This method can be called before or after <code>init</code>.
 *
 * @param {String} waveId identifies the wave to load.
 * @param {Function} opt_callback to callback when done (optional).
 * @export
 */
window.google.wave.WavePanel.prototype.loadWave = function(waveId, opt_callback) {
  if (this.init_) {
    var callbackId = window.google.wave.WavePanel.pushCallback_(opt_callback);
    gadgets.rpc.call(
        this.getFrameId(), 'load_wave', null, callbackId, waveId);
  } else {
    this.initWaveId_ = waveId;
    // Load into target div if one is specified
    if (this.uiConfig_.target) {
      this.init(this.uiConfig_.target);
    }
  }
};

/**
 * Sets the edit mode for the wave. This method currently affects only the
 * root blip.
 *
 * @param value Anything that evaluates to true will make the wave enter edit
 *              mode. Anything else will make it leave edit mode.
 */
window.google.wave.WavePanel.prototype.setEditMode = function(value) {
  if (!this.init_) {
    throw 'Init not called.';
  }
  value = !!value ? "true" : "false";
  gadgets.rpc.call(this.getFrameId(), 'set_edit_mode', null, value);
}
