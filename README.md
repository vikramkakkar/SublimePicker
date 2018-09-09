# SublimePicker

A customizable view that provisions picking of a date, time & recurrence option, all from a single user-interface. You can also view 'SublimePicker' as a collection of material-styled (API 23) DatePicker, TimePicker & RecurrencePicker, backported to API 14.

With the release of version 2, SublimePicker now allows date-range selection.

All three components have been converted to views, which enables us to add/embed them anywhere - add them to a 'Dialog', a 'PopupWindow', or use them as any regular android widget. 

Almost all styling possibilities have been exposed through attrs. If something has been missed, please bring it to my attention.

SublimePicker provides a single point of entry to all three pickers. What I mean is that once SublimePicker is available for interaction, the user can select a date, a time & a recurrence option without leaving the picker.

Gradle dependency
-----------------

`compile 'com.appeaser.sublimepickerlibrary:sublimepickerlibrary:2.1.2'`

Walkthrough
-----------
Following screenshots have been taken from the sample application: [<img alt="Get it on Google Play" height="45px" src="https://play.google.com/intl/en_us/badges/images/apps/en-play-badge-border.png" />][1]

Launching into **DatePicker**:

<p align="center">
<img src="https://github.com/vikramkakkar/SublimePicker/blob/master/img/date_picker_v2.png?raw=true" width="497" height="1000" />
</p>

Version 2 allows date-range selection using `SublimeOptions#setCanPickDateRange(boolean)`. Picking date range is _one fluent gesture_ which begins with a long-press on the intended start-date, followed by a drag onto the intended end-date. The range can span as many days, months or years, as needed. During a drag, approaching the left/right edge of date picker scrolls the previous/next month into view.  

Date range selection:

<p align="center">
<img src="https://github.com/vikramkakkar/SublimePicker/blob/master/img/date_picker_date_range_v2.png?raw=true" width="497" height="1000" />
</p>

Landscape:

<p align="center">
<img src="https://github.com/vikramkakkar/SublimePicker/blob/master/img/date_picker_date_range_land_v2.png?raw=true" width="800" height="397" />
</p>

Date range selection spanning multiple months:

<p align="center">
<img src="https://github.com/vikramkakkar/SublimePicker/blob/master/img/date_picker_date_range_spanned_v2.png?raw=true" width="497" height="1000" />
</p>

Button at bottom-left corner can be used to switch to **TimePicker**: 

<p align="center">
<img src="https://github.com/vikramkakkar/SublimePicker/blob/master/img/time_picker_v2.png?raw=true" width="497" height="1000" />
</p>

Landscape:

<p align="center">
<img src="https://github.com/vikramkakkar/SublimePicker/blob/master/img/time_picker_land_v2.png?raw=true" width="800" height="397" />
</p>

The overflow button at top-right opens the **RecurrencePicker**:

<p align="center">
<img src="https://github.com/vikramkakkar/SublimePicker/blob/master/img/recurrence_picker_v2.png?raw=true" width="497" height="1000" />
</p>

Choosing **Custom...** from this menu brings you to **RecurrenceOptionCreator**:

<p align="center">
<img src="https://github.com/vikramkakkar/SublimePicker/blob/master/img/recurrence_option_creator_v2.png?raw=true" width="497" height="1000" />
</p>

Picking **Until a date** from the bottom spinner & clicking on the date shows a stripped down version of DatePicker:

<p align="center">
<img src="https://github.com/vikramkakkar/SublimePicker/blob/master/img/recurrence_option_creator_end_date_v2.png?raw=true" width="497" height="1000" />
</p>

**Sample Application**:

<p align="center">
<img src="https://github.com/vikramkakkar/SublimePicker/blob/master/img/sampler_v2.png?raw=true" width="497" height="1000" />
</p>

Results of the selection in **sample application** (single date):

<p align="center">
<img src="https://github.com/vikramkakkar/SublimePicker/blob/master/img/sampler_results_single_date_v2.png?raw=true" width="497" height="1000" />
</p>

Results of the selection in **sample application** (date range):

<p align="center">
<img src="https://github.com/vikramkakkar/SublimePicker/blob/master/img/sampler_results_date_range_v2.png?raw=true" width="497" height="1000" />
</p>

**How-to**s will be added in a day or two...

License
-------
    Copyright (c) 2018 Vikram Kakkar

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
	
	
	
[1]: https://play.google.com/store/apps/details?id=com.appeaser.sublimepicker
